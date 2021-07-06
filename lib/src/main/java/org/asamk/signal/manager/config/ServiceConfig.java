package org.asamk.signal.manager.config;

import org.signal.zkgroup.internal.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.signalservice.api.account.AccountAttributes;
import org.whispersystems.signalservice.api.push.TrustStore;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;


class LoggingInterceptor implements Interceptor {
    private final static Logger logger = LoggerFactory.getLogger("root");
    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();
        logger.info(String.format("Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()));

        Response response = chain.proceed(request);

        long t2 = System.nanoTime();
        logger.info(String.format("Received response for %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6d, response.headers()));

        return response;
    }
}

public class ServiceConfig {

    public final static int PREKEY_MINIMUM_COUNT = 20;
    public final static int PREKEY_BATCH_SIZE = 100;
    public final static int MAX_ATTACHMENT_SIZE = 150 * 1024 * 1024;
    public final static long MAX_ENVELOPE_SIZE = 0;
    public final static long AVATAR_DOWNLOAD_FAILSAFE_MAX_SIZE = 10 * 1024 * 1024;
    public final static boolean AUTOMATIC_NETWORK_RETRY = true;

    private final static KeyStore iasKeyStore;

    public static final AccountAttributes.Capabilities capabilities;

    static {
        boolean zkGroupAvailable;
        try {
            Native.serverPublicParamsCheckValidContentsJNI(new byte[]{});
            zkGroupAvailable = true;
        } catch (Throwable ignored) {
            zkGroupAvailable = false;
        }
        capabilities = new AccountAttributes.Capabilities(false, zkGroupAvailable, false, zkGroupAvailable);

        try {
            TrustStore contactTrustStore = new IasTrustStore();

            var keyStore = KeyStore.getInstance("BKS");
            keyStore.load(contactTrustStore.getKeyStoreInputStream(),
                    contactTrustStore.getKeyStorePassword().toCharArray());

            iasKeyStore = keyStore;
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static boolean isSignalClientAvailable() {
        try {
            org.signal.client.internal.Native.DisplayableFingerprint_Format(new byte[30], new byte[30]);
            return true;
        } catch (UnsatisfiedLinkError ignored) {
            return false;
        }
    }

    public static AccountAttributes.Capabilities getCapabilities() {
        return capabilities;
    }

    public static KeyStore getIasKeyStore() {
        return iasKeyStore;
    }

    public static ServiceEnvironmentConfig getServiceEnvironmentConfig(
            ServiceEnvironment serviceEnvironment, String userAgent
    ) {
        final Interceptor userAgentInterceptor = chain -> chain.proceed(chain.request()
                .newBuilder()
                .header("User-Agent", userAgent)
                .build());

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        final var interceptors = List.of(userAgentInterceptor, logging);

        switch (serviceEnvironment) {
            case LIVE:
                return new ServiceEnvironmentConfig(LiveConfig.createDefaultServiceConfiguration(interceptors),
                        LiveConfig.getUnidentifiedSenderTrustRoot(),
                        LiveConfig.createKeyBackupConfig(),
                        LiveConfig.getCdsMrenclave());
            case SANDBOX:
                return new ServiceEnvironmentConfig(SandboxConfig.createDefaultServiceConfiguration(interceptors),
                        SandboxConfig.getUnidentifiedSenderTrustRoot(),
                        SandboxConfig.createKeyBackupConfig(),
                        SandboxConfig.getCdsMrenclave());
            default:
                throw new IllegalArgumentException("Unsupported environment");
        }
    }
}
