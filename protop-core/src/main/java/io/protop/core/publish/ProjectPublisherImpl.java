package io.protop.core.publish;

import com.google.common.collect.ImmutableMap;
import io.protop.core.Context;
import io.protop.core.Environment;
import io.protop.core.auth.AuthService;
import io.protop.core.auth.AuthToken;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.AggregatedManifest;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.utils.HttpUtils;
import io.protop.utils.RegistryUtils;
import io.protop.utils.UriUtils;
import io.reactivex.Completable;
import lombok.AllArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@AllArgsConstructor
public class ProjectPublisherImpl implements ProjectPublisher {

    private static final Logger logger = Logger.getLogger(ProjectPublisherImpl.class);

    private final Context context;
    private final AuthService<?> authService;

    @Override
    public Completable publish(PublishableProject project) {
        return Completable.create(emitter -> {
            URI registry = context.getRc().getRepositoryUri();
            Manifest manifest = project.getManifest();
            PublishableProject.CompressedArchiveDetails archiveDetails = project.compressAndZip();

            Optional<AuthToken> token = Optional.ofNullable(
                    authService.getStoredToken(registry).blockingGet());

            HttpClient client = token
                    .map(HttpUtils::createHttpClientWithToken)
                    .orElseGet(HttpUtils::createHttpClient);

            try {
                URI coordinateUri = createFullPublishUri(registry, manifest);
                PublishableManifest publishableManifest = buildPublishableManifest(manifest, archiveDetails, coordinateUri);
                AggregatedManifest aggregatedManifest = buildManifest(publishableManifest, archiveDetails);
                publish(aggregatedManifest, client, coordinateUri).subscribe(
                        emitter::onComplete,
                        emitter::onError).dispose();
            } catch (URISyntaxException e) {
                logger.error("Failed to upload project.", e);
                emitter.onError(e);
            }
        });
    }

    private URI createFullPublishUri(URI registry, Manifest manifest) throws URISyntaxException {
        return UriUtils.appendPathSegments(
                registry,
                manifest.getOrganization(),
                manifest.getName());
    }

    private Completable publish(AggregatedManifest aggregatedManifest, HttpClient client, URI assetUri) {
        return Completable.create(emitter -> {
            HttpPut put = new HttpPut(assetUri);
            String requestEntity = Environment.getInstance().getObjectMapper()
                    .writeValueAsString(aggregatedManifest);
            put.setEntity(new StringEntity(requestEntity));

            HttpResponse response = client.execute(put);
            logger.info("Publish response status: {}.", response.getStatusLine().getStatusCode());

            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                emitter.onComplete();;
            } else {
                HttpEntity responseEntity = response.getEntity();
                Optional.ofNullable(responseEntity).ifPresent(entity -> {
                    try {
                        String content = EntityUtils.toString(entity);
                        // TODO map to a descriptive error from some backend code (TODO there as well)
                        logger.info("Publish response entity: {}.", content);
                    } catch (IOException ioe) {
                        emitter.onError(ioe);
                    }
                });
                // TODO actually deserialize the response, since this may not always be the reason.
                //  This is just a temporary bandaid.
                emitter.onError(new ServiceException(ServiceError.VERSION_ALREADY_PUBLISHED));
            }
        });
    }

    private PublishableManifest buildPublishableManifest(Manifest manifest,
                                                         PublishableProject.CompressedArchiveDetails archiveDetails,
                                                         URI coordinateUri) throws URISyntaxException {
        String tarballName = RegistryUtils.createTarballName(
                new ProjectCoordinate(manifest.getOrganization(), manifest.getName()),
                manifest.getVersion());
        String tarballUri = new URIBuilder(coordinateUri)
                .setPath(coordinateUri.getPath() + "/-/" + tarballName)
                .build()
                .toString();
        return PublishableManifest.builder()
                .name(manifest.getName())
                .version(manifest.getVersion())
                .dependencies(manifest.getDependencies())
                .organization(manifest.getOrganization())
                .readme(manifest.getReadme())
                .description(manifest.getDescription())
                .homepage(manifest.getHomepage())
                .keywords(manifest.getKeywords())
                .license(manifest.getLicense())
                .dist(PublishableManifest.Dist.builder()
                        .fileCount(archiveDetails.getFilecount())
                        .integrity(archiveDetails.getIntegrity())
                        .shasum(archiveDetails.getShasum())
                        .unpackedSize(archiveDetails.getUnpackedSize())
                        .tarball(tarballUri)
                        .build())
                .build();
    }

    private AggregatedManifest buildManifest(PublishableManifest manifest,
                                             PublishableProject.CompressedArchiveDetails archiveDetails) {
        // TODO fetch existing first, so we can mergeOver with this...
        // because the manifest will be the root manifest of all versions,
        // and the "versions" is really where the unique versions will be.
        // this is really a sort of patch operation under the hood

        ProjectCoordinate id = new ProjectCoordinate(manifest.getOrganization(), manifest.getName());

        AggregatedManifest.Attachment tarAttachment;
        try {
            tarAttachment = AggregatedManifest.Attachment.of(archiveDetails.getLocation().toFile());
        } catch (IOException e) {
            throw new ServiceException("Failed to build publishable manifest.", e);
        }

        // TODO patch this later, doing a GET first and merging this onto that.
        //  When we do that, make sure the new version isn't already published?
        AggregatedManifest.AggregatedManifestBuilder builder = AggregatedManifest.builder()
                .description(manifest.getDescription())
                .id(id.toString())
                .name(id.getProjectId())
                .org(id.getOrganizationId())
                .readme(manifest.getReadme())
                .version(manifest.getVersion())
                .distTags(ImmutableMap.of())
                .versions(ImmutableMap.of(
                        manifest.getVersion(),
                        manifest))
                .attachments(ImmutableMap.of(
                        RegistryUtils.createTarballName(id, manifest.getVersion()),
                        tarAttachment));

        return builder.build();
    }
}
