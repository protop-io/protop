package io.protop.core.publishing;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.protop.core.auth.BasicCredentials;
import io.protop.core.auth.CredentialService;
import io.protop.core.config.Configuration;
import io.protop.core.config.ProjectId;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import lombok.AllArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

@AllArgsConstructor
public class ProjectPublisherImpl implements ProjectPublisher {

    private static final Logger logger = Logger.getLogger(ProjectPublisherImpl.class);

    private final CredentialService<BasicCredentials> credentialService;

    // https://github.com/npm/libnpmpublish/blob/latest/publish.js
    // ^ shows how to build a manifest

    @Override
    public void publish(PublishableProject project, URI registry) {
        Path archiveLocation = project.compressAndZip();
        File file = archiveLocation.toFile();

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("")

        CredentialsProvider provider = new BasicCredentialsProvider();
        Optional<BasicCredentials> encoded = Optional.ofNullable(
                credentialService.getStoredCredentials(registry).blockingGet());

        // TODO improve this
        encoded.ifPresent(creds -> {
            String decoded = new String(
                    Base64.getDecoder().decode(creds.getBasic()),
                    StandardCharsets.UTF_8);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(decoded);
            provider.setCredentials(AuthScope.ANY, credentials);

            HttpClient client = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();

            HttpPut put = new HttpPut(registry);

            try {
                HttpResponse response = client.execute(put);

                logger.info("Publish response status: {}.", response.getStatusLine().getStatusCode());

                HttpEntity entity = response.getEntity();
                Optional.ofNullable(entity).ifPresent(e -> {
                    try {
                        String content = EntityUtils.toString(e);
                        logger.info("Publish response entity: {}.", content);
                    } catch (IOException ioe) {
                        // ...
                    }
                });

            } catch (IOException e) {
                logger.error("Failed to upload project.", e);
            }
        });

        if (encoded.isEmpty()) {
            logger.info("Could not find credentials for {}.", registry);
        }
    }

    private Manifest buildManifest(Configuration configuration, File tar) {
        // TODO fetch existing first, so we can merge with this...
        // because the manifest will be the root manifest of all versions,
        // and the "versions" is really where the unique versions will be.
        // this is really a sort of patch operation under the hood

        ProjectId id = new ProjectId(configuration.getOrganization(), configuration.getName());

        Manifest.Attachment tarAttachment;
        try {
            tarAttachment = Manifest.Attachment.of(tar);
        } catch (IOException e) {
            throw new ServiceException("Failed to prepare package", e);
        }

        Manifest.ManifestBuilder builder = Manifest.builder()
                .description("TODO")
                .id(id.toString())
                .org(id.getOrganization())
                .name(id.getProject())
                .readme("TODO")
                .version(configuration.getVersion())
                .distTags(ImmutableMap.of())
                .versions(ImmutableMap.of())
                .attachments(ImmutableMap.of(createTarName(configuration), tarAttachment));

        return builder.build();
    }

    private String createTarName(Configuration configuration) {
        return String.join("-",
                configuration.getOrganization(),
                configuration.getName(),
                configuration.getVersion().toString());
    }
}
