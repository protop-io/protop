package io.protop.core.publish;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.protop.core.Context;
import io.protop.core.RuntimeConfiguration;
import io.protop.core.auth.AuthService;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.Manifest;
import io.protop.registry.data.Package;
import io.protop.registry.services.Publish;
import io.protop.registry.services.PublishServiceGrpc;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import lombok.AllArgsConstructor;

import java.io.FileInputStream;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ProjectPublisherImpl implements ProjectPublisher {

    private static final Logger logger = Logger.getLogger(ProjectPublisherImpl.class);

    private final Context context;
    private final AuthService<?> authService;

    private URI getPublishURI() {
        RuntimeConfiguration rc = context.getRc();
        URI registry = Optional.ofNullable(rc.getPublishRepositoryUri())
                .orElse(rc.getRepositoryUri());
        return Optional.ofNullable(registry)
                .orElseThrow(() -> new RuntimeException("Could not resolve the registry URL."));
    }

    private PublishServiceGrpc.PublishServiceStub createPublishServiceStub(URI publishURI) {
        Channel channel = ManagedChannelBuilder
                .forTarget(publishURI.toString())
                .build();
        return PublishServiceGrpc
                .newStub(channel);
    }

//    private Maybe<AuthToken> getAuthToken(URI uri) {
//        return authService.getStoredToken(uri)
//                .switchIfEmpty(Maybe.defer(authService.authorize("TODO")));
//    }

    @Override
    public Completable publish(PublishableProject project) {
        return Completable.create(emitter -> {
            URI publishURI = getPublishURI();
            Manifest manifest = project.getManifest();
            PublishableProject.CompressedArchiveDetails archiveDetails = project.compressAndZip();
            Publish.Manifest publishableManifest = buildPublishableManifest(manifest, archiveDetails);

            PublishServiceGrpc.PublishServiceStub publishServiceStub = createPublishServiceStub(publishURI);
            StreamObserver<Publish.PublishRequest> requestObserver = publishServiceStub.publish(
                    createResponseObserver(emitter));

            requestObserver.onNext(Publish.PublishRequest.newBuilder()
                    .setManifest(publishableManifest)
                    .build());

            FileInputStream fis = new FileInputStream(archiveDetails.getLocation().toFile());
            byte[] buffer = new byte[1024*1024];
            while (fis.read(buffer) > 0) {
                requestObserver.onNext(Publish.PublishRequest.newBuilder()
                        .setData(Package.DataChunk.newBuilder()
                                .setData(ByteString.copyFrom(buffer))
                                .build())
                        .build());
            }
            requestObserver.onCompleted();
            fis.close();
            emitter.onComplete();
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
                // Nothing to do.
            }
        };
    }

    private Publish.Manifest buildPublishableManifest(Manifest manifest,
                                                         PublishableProject.CompressedArchiveDetails archiveDetails) {
        return Publish.Manifest.newBuilder()
                .setOrganizationId(manifest.getOrganization())
                .setProjectId(manifest.getName())
                .setDescription(manifest.getDescription())
                .setVersion(manifest.getVersion().toString())
                .addAllDependencies(manifest.getDependencies()
                        .getValues()
                        .entrySet()
                        .stream()
                        .map(entry -> Publish.Dependency.newBuilder()
                                .setPackageId(entry.getKey().toString())
                                .setSource(entry.getValue().toString())
                                .build())
                        .collect(Collectors.toList()))
                .setReadme(manifest.getReadme())
                .setHomepage(manifest.getHomepage().toString())
                .setLicense(manifest.getLicense())
                .addAllKeywords(manifest.getKeywords())
                .build();
    }
}
