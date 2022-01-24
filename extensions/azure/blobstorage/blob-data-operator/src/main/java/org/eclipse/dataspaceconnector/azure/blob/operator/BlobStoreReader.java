package org.eclipse.dataspaceconnector.azure.blob.operator;

import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.common.azure.BlobStoreApi;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataReader;

import java.io.ByteArrayInputStream;

public class BlobStoreReader implements DataReader {

    private final BlobStoreApi blobStoreApi;

    public BlobStoreReader(BlobStoreApi blobStoreApi) {
        this.blobStoreApi = blobStoreApi;
    }

    @Override
    public boolean canHandle(String type) {
        return AzureBlobStoreSchema.TYPE.equals(type);
    }

    @Override
    public Result<ByteArrayInputStream> read(DataAddress source) {
        var account = source.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var container = source.getProperty(AzureBlobStoreSchema.CONTAINER_NAME);
        var blobName = source.getProperty(AzureBlobStoreSchema.BLOB_NAME);
        return Result.success(new ByteArrayInputStream(blobStoreApi.getBlob(account, container, blobName)));
    }
}
