package io.protop.core.publish;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.protop.core.Context;
import io.protop.core.RuntimeConfiguration;
import io.protop.core.auth.AuthService;
import io.protop.core.grpc.AuthTokenCallCredentials;
import io.protop.core.grpc.GrpcService;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.Manifest;
import io.protop.registry.data.Package;
import io.protop.registry.services.Publish;
import io.protop.registry.services.PublishServiceGrpc;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import lombok.AllArgsConstructor;

import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ProjectPublisherImpl implements ProjectPublisher {

    private static final Logger logger = Logger.getLogger(ProjectPublisherImpl.class);

    private final Context context;
    private final AuthService<?> authService;
    private final GrpcService grpcService;

    private URL getPublishURL() {
        RuntimeConfiguration rc = context.getRc();
        String urlFromRc = Optional.ofNullable(rc.getPublishRepositoryUrl())
                .orElse(rc.getRepositoryUrl());

        if (Objects.isNull(urlFromRc)) {
            throw new RuntimeException("Publish URL not found.");
        }

        try {
            return new URL(urlFromRc);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to parse publish URL.", e);
        }
    }

    private PublishServiceGrpc.PublishServiceStub createPublishServiceStub(URL publishURL) {
        Channel channel = ManagedChannelBuilder
                .forAddress(publishURL.getHost(), publishURL.getPort())
                .usePlaintext()
                .build();
        return PublishServiceGrpc.newStub(channel);
    }

    @Override
    public Completable publish(PublishableProject project) {
        return Completable.create(emitter -> {
            URL publishURI = getPublishURL();
            AuthTokenCallCredentials credentials = grpcService.getAuthCredentials(publishURI);

            Manifest manifest = project.getManifest();
            PublishableProject.CompressedArchiveDetails archiveDetails = project.compressAndZip();
            Publish.Manifest publishableManifest = buildPublishableManifest(manifest, archiveDetails);

            PublishServiceGrpc.PublishServiceStub publishServiceStub = createPublishServiceStub(publishURI)
                    .withCallCredentials(credentials);
            StreamObserver<Publish.PublishRequest> requestObserver = publishServiceStub.publish(
                    createResponseObserver(emitter));

            requestObserver.onNext(Publish.PublishRequest.newBuilder()
                    .setManifest(publishableManifest)
                    .build());

            FileInputStream fis = new FileInputStream(archiveDetails.getLocation().toFile());
            byte[] buffer = new byte[512];
            while (fis.read(buffer) > 0) {
                requestObserver.onNext(Publish.PublishRequest.newBuilder()
                        .setData(Package.DataChunk.newBuilder()
                                .setData(ByteString.copyFrom(buffer))
                                .build())
                        .build());
            }
            requestObserver.onCompleted();
            fis.close();
        });
    }

    private StreamObserver<Publish.PublishStatus> createResponseObserver(CompletableEmitter emitter) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Publish.PublishStatus status) {
                // TODO something with status
            }
            @Override
            public void onError(Throwable t) {
                emitter.onError(t);
            }
            @Override
            public void onCompleted() {
                emitter.onComplete();
            }
        };
    }

    private Publish.Manifest buildPublishableManifest(Manifest manifest,
                                                         PublishableProject.CompressedArchiveDetails archiveDetails) {
        Publish.Manifest.Builder builder = Publish.Manifest.newBuilder();

        Optional.ofNullable(manifest.getOrganization()).ifPresent(builder::setOrganization);
        Optional.ofNullable(manifest.getName()).ifPresent(builder::setProject);
        Optional.ofNullable(manifest.getDescription()).ifPresent(builder::setDescription);
        Optional.ofNullable(manifest.getVersion()).ifPresent(version -> builder.setVersion(version.toString()));
        Optional.ofNullable(manifest.getReadme()).ifPresent(builder::setReadme);
        Optional.ofNullable(manifest.getHomepage()).ifPresent(homepage -> builder.setHomepage(homepage.toString()));
        Optional.ofNullable(manifest.getLicense()).ifPresent(builder::setLicense);
        Optional.ofNullable(manifest.getKeywords()).ifPresent(builder::addAllKeywords);
        Optional.ofNullable(manifest.getDependencies()).ifPresent(dependencyMap ->
                builder.addAllDependencies(dependencyMap.getValues()
                        .entrySet()
                        .stream()
                        .map(entry -> Publish.Dependency.newBuilder()
                                .setPackageId(entry.getKey().toString())
                                .setSource(entry.getValue().toString())
                                .build())
                        .collect(Collectors.toList())));

        return builder.build();
    }
}
