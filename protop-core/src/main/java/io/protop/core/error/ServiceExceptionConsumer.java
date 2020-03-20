package io.protop.core.error;

import io.protop.core.auth.AuthenticationFailed;
import io.protop.core.manifest.InvalidDependencyName;
import io.protop.core.manifest.ManifestNotFound;
import io.protop.core.manifest.revision.InvalidRevision;
import io.protop.core.publish.PublishFailed;
import io.protop.core.storage.StorageException;
import io.protop.core.sync.IncompleteSync;

public interface ServiceExceptionConsumer {

    void consume(PackageNotFound packageNotFound);

    void consume(IncompleteSync incompleteSync);

    void consume(ProjectAlreadyCreated projectAlreadyCreated);

    void consume(StorageException storageException);

    void consume(InvalidDependencyName invalidDependencyName);

    void consume(PublishFailed publishFailed);

    void consume(AuthenticationFailed authenticationFailed);

    void consume(ManifestNotFound manifestNotFound);

    void consume(InvalidRevision invalidRevision);
}
