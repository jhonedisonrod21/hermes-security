package co.com.hermes.calendar.auth.config;

import co.com.hermes.calendar.auth.identity.HermesPrincipalResolver;
import co.com.hermes.calendar.auth.identity.HermesUserPrincipal;
import co.com.hermes.calendar.auth.identity.IdentityAuthProperties;
import co.com.hermes.calendar.shared.contract.CredentialVerificationResponse;
import co.com.hermes.calendar.shared.security.HermesInternalHeaders;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(IdentityAuthProperties.class)
public class AuthorizationServerConfig {

    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            @Value("${hermes.web.login-url:http://127.0.0.1:5173}") String loginUrl
    ) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        http
                .securityMatcher(endpointsMatcher)
                .with(authorizationServerConfigurer, authorizationServer -> authorizationServer.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint(loginUrl),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            @Value("${hermes.web.login-url:http://127.0.0.1:5173}") String loginUrl
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/session/login"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/session/login",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/error",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint(loginUrl),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
            IdentityAuthProperties properties,
            HermesPrincipalResolver principalResolver,
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        RestClient identityClient = restClientBuilder.baseUrl(properties.baseUrl()).build();

        return context -> {
            HermesUserPrincipal user = resolveHermesUserPrincipal(context, properties, principalResolver, identityClient);
            if (user != null) {
                context.getClaims()
                        .subject(user.getUserId().toString())
                        .claim("user_id", user.getUserId())
                        .claim("preferred_username", user.getUsername())
                        .claim("email", user.getEmail())
                        .claim("account_scope", user.getScope().name())
                        .claim("roles", user.getRoles())
                        .claim("permissions", user.getPermissions());
                if (user.getTenantId() != null) {
                    context.getClaims()
                            .claim("tenant_id", user.getTenantId())
                            .claim("tenant_slug", user.getTenantSlug())
                            .claim("tenant_name", user.getTenantName());
                }
            }
        };
    }

    private static HermesUserPrincipal resolveHermesUserPrincipal(
            JwtEncodingContext context,
            IdentityAuthProperties properties,
            HermesPrincipalResolver principalResolver,
            RestClient identityClient
    ) {
        Authentication principal = context.getPrincipal();
        if (principal != null && principal.getPrincipal() instanceof HermesUserPrincipal user) {
            return user;
        }
        if (context.getAuthorization() == null || !StringUtils.hasText(context.getAuthorization().getPrincipalName())) {
            return null;
        }

        CredentialVerificationResponse user = identityClient.get()
                .uri("/internal/auth/users/{username}", context.getAuthorization().getPrincipalName())
                .header(INTERNAL_KEY_HEADER, properties.internalApiKey())
                .retrieve()
                .body(CredentialVerificationResponse.class);

        return principalResolver.resolve(user).orElse(null);
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(
            @Value("${hermes.auth.jwk.key-id:}") String keyId,
            @Value("${hermes.auth.jwk.public-key-pem:}") String publicKeyPem,
            @Value("${hermes.auth.jwk.private-key-pem:}") String privateKeyPem
    ) {
        RSAKey rsaKey = StringUtils.hasText(publicKeyPem) && StringUtils.hasText(privateKeyPem)
                ? loadRsa(keyId, publicKeyPem, privateKeyPem)
                : generateRsa(keyId);
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(@Value("${hermes.auth.issuer-uri}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    private static RSAKey generateRsa(String keyId) {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(StringUtils.hasText(keyId) ? keyId : UUID.randomUUID().toString())
                .build();
    }

    private static RSAKey loadRsa(String keyId, String publicKeyPem, String privateKeyPem) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodePem(publicKeyPem)));
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodePem(privateKeyPem)));
            return new RSAKey.Builder((RSAPublicKey) publicKey)
                    .privateKey((RSAPrivateKey) privateKey)
                    .keyID(StringUtils.hasText(keyId) ? keyId : "hermes-auth-key")
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load configured RSA key pair", ex);
        }
    }

    private static byte[] decodePem(String pem) {
        String normalized = new String(pem.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate RSA key pair", ex);
        }
    }
}
