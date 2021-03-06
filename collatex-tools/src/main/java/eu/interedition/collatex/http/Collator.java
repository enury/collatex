package eu.interedition.collatex.http;

import eu.interedition.collatex.jung.JungVariantGraph;
import eu.interedition.collatex.simple.SimpleCollation;
import eu.interedition.collatex.simple.SimpleToken;
import eu.interedition.collatex.simple.SimpleVariantGraphSerializer;
import eu.interedition.collatex.simple.SimpleWitness;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Header;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Collator {

  private final int maxCollationSize;
  private final String dotPath;
  private final ExecutorService collationThreads;
  private final ExecutorService processThreads = Executors.newCachedThreadPool();

  public Collator(int maxParallelCollations, int maxCollationSize, String dotPath) {
    this.collationThreads = Executors.newFixedThreadPool(maxParallelCollations, new ThreadFactory() {
      private final AtomicLong counter = new AtomicLong();

      @Override
      public Thread newThread(Runnable r) {
        final Thread t = new Thread(r, "collator-" + counter.incrementAndGet());
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
      }
    });

    this.maxCollationSize = maxCollationSize;
    this.dotPath = dotPath;
  }

  public void service(Request request, Response response) throws Exception {
    final Deque<String> path = path(request);
    if (path.isEmpty() || !"collate".equals(path.pop())) {
      response.sendError(404);
      return;
    }

    final SimpleCollation collation = JsonProcessor.read(request.getInputStream());
    if (maxCollationSize > 0) {
      for (SimpleWitness witness : collation.getWitnesses()) {
        final int witnessLength = witness.getTokens().stream()
                .filter(t -> t instanceof SimpleToken).map(t -> (SimpleToken) t)
                .collect(Collectors.summingInt(t -> t.getContent().length()));
        if (witnessLength > maxCollationSize) {
          response.sendError(413, "Request Entity Too Large");
          return;
        }
      }
    }

    response.suspend(60, TimeUnit.SECONDS, new EmptyCompletionHandler<>());
    collationThreads.submit(() -> {
      try {
        final JungVariantGraph graph = new JungVariantGraph();
        collation.collate(graph);

        // CORS support
        response.setHeader("Access-Control-Allow-Origin", Optional.ofNullable(request.getHeader("Origin")).orElse("*"));
        response.setHeader("Access-Control-Allow-Methods", Optional.ofNullable(request.getHeader("Access-Control-Request-Method")).orElse("GET, POST, HEAD, OPTIONS"));
        response.setHeader("Access-Control-Allow-Headers", Optional.ofNullable(request.getHeader("Access-Control-Request-Headers")).orElse("Content-Type, Accept, X-Requested-With"));
        response.setHeader("Access-Control-Max-Age", "86400");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        final String clientAccepts = Optional.ofNullable(request.getHeader(Header.Accept)).orElse("");

        if (clientAccepts.contains("text/plain")) {
          response.setContentType("text/plain");
          response.setCharacterEncoding("utf-8");
          try (final Writer out = response.getWriter()) {
            new SimpleVariantGraphSerializer(graph).toDot(out);
          }
          response.resume();

        } else if (clientAccepts.contains("application/tei+xml")) {
          XMLStreamWriter xml = null;
          try {
            response.setContentType("application/tei+xml");
            try (OutputStream responseStream = response.getOutputStream()) {
              xml = XMLOutputFactory.newInstance().createXMLStreamWriter(responseStream);
              xml.writeStartDocument();
              new SimpleVariantGraphSerializer(graph).toTEI(xml);
              xml.writeEndDocument();
            } finally {
              if (xml != null) {
                xml.close();
              }
            }
            response.resume();
          } catch (XMLStreamException e) {
            e.printStackTrace();
          }
        } else if (clientAccepts.contains("application/graphml+xml")) {
          XMLStreamWriter xml = null;
          try {
            response.setContentType("application/graphml+xml");
            try (OutputStream responseStream = response.getOutputStream()) {
              xml = XMLOutputFactory.newInstance().createXMLStreamWriter(responseStream);
              xml.writeStartDocument();
              new SimpleVariantGraphSerializer(graph).toGraphML(xml);
              xml.writeEndDocument();
            } finally {
              if (xml != null) {
                xml.close();
              }
            }
            response.resume();
          } catch (XMLStreamException e) {
            e.printStackTrace();
          }
        } else if (clientAccepts.contains("image/svg+xml")) {
          if (dotPath == null) {
            response.sendError(204);
            response.resume();
          } else {
            final StringWriter dot = new StringWriter();
            new SimpleVariantGraphSerializer(graph).toDot(dot);

            final Process dotProc = new ProcessBuilder(dotPath, "-Grankdir=LR", "-Gid=VariantGraph", "-Tsvg").start();
            final StringWriter errors = new StringWriter();
            CompletableFuture.allOf(
                    CompletableFuture.runAsync(() -> {
                      final char[] buf = new char[8192];
                      try (final Reader errorStream = new InputStreamReader(dotProc.getErrorStream())) {
                        int len;
                        while ((len = errorStream.read(buf)) >= 0) {
                          errors.write(buf, 0, len);
                        }
                      } catch (IOException e) {
                        throw new CompletionException(e);
                      }
                    }, processThreads),
                    CompletableFuture.runAsync(() -> {
                      try (final Writer dotProcStream = new OutputStreamWriter(dotProc.getOutputStream(), "UTF-8")) {
                        dotProcStream.write(dot.toString());
                      } catch (IOException e) {
                        throw new CompletionException(e);
                      }
                    }, processThreads),
                    CompletableFuture.runAsync(() -> {
                      response.setContentType("image/svg+xml");
                      final byte[] buf = new byte[8192];
                      try (final InputStream in = dotProc.getInputStream(); final OutputStream out = response.getOutputStream()) {
                        int len;
                        while ((len = in.read(buf)) >= 0) {
                          out.write(buf, 0, len);
                        }
                      } catch (IOException e) {
                        throw new CompletionException(e);
                      }
                    }, processThreads),
                    CompletableFuture.runAsync(() -> {
                      try {
                        if (dotProc.waitFor() != 0) {
                          throw new CompletionException(new IllegalStateException(errors.toString()));
                        }
                      } catch (InterruptedException e) {
                        throw new CompletionException(e);
                      }
                    }, processThreads)
            ).exceptionally(t -> {
              t.printStackTrace();
              return null;
            }).thenRunAsync(response::resume, processThreads);
          }
        } else {
          response.setContentType("application/json");
          try (final OutputStream responseStream = response.getOutputStream()) {
            JsonProcessor.write(graph, responseStream);
          }
          response.resume();
        }
      } catch (IOException e) {
        // FIXME: ignored
      }
    });
  }

  private static Deque<String> path(Request request) {
    return Pattern.compile("/+").splitAsStream(Optional.ofNullable(request.getPathInfo()).orElse(""))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(ArrayDeque::new));
  }

}
