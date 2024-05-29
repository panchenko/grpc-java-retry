package io.grpc.examples.retrying;

import com.google.common.util.concurrent.Uninterruptibles;
import io.grpc.servlet.GrpcServlet;
import io.grpc.servlet.ServletServerBuilder;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http2.Http2Protocol;

import static java.nio.file.Files.createDirectory;

public class TomcatServer {
    public static void main(String[] args) throws IOException, LifecycleException {
        Path tempDir = Files.createTempDirectory("test");
        try {
            Tomcat tomcatServer = new Tomcat();
            tomcatServer.setPort(50051);
            tomcatServer.setBaseDir(createDirectory(tempDir.resolve("root")).toString());
            Context ctx = tomcatServer.addContext("", createDirectory(tempDir.resolve("context")).toString());
            GrpcServlet grpcServlet = new ServletServerBuilder()
                    .addService(new RetryingHelloWorldServer.GreeterImpl())
                    .intercept(new ForceTrailersServerInterceptor())
                    .buildServlet();
            Tomcat.addServlet(ctx, "TomcatTest", grpcServlet).setAsyncSupported(true);
            ctx.addServletMappingDecoded("/*", "TomcatTest");
            tomcatServer.getConnector().addUpgradeProtocol(new Http2Protocol());
            tomcatServer.start();

            Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MINUTES);
        } finally {
            delete(tempDir);
        }
    }

    private static void delete(Path folder) {
        try {
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    try {
                        Files.deleteIfExists(dir);
                    } catch (IOException e) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
        }
    }
}
