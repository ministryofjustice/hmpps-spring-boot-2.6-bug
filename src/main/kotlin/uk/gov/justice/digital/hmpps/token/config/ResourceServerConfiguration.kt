package uk.gov.justice.digital.hmpps.token.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

@Configuration
@EnableWebSecurity
open class ResourceServerConfiguration : WebSecurityConfigurerAdapter() {
  public override fun configure(http: HttpSecurity) {
    http.sessionManagement()
      .sessionCreationPolicy(STATELESS) // Can't have CSRF protection as requires session
      .and().csrf().disable()
      .authorizeRequests { auth ->
        auth.antMatchers(
          "/favicon.ico", "/csrf", "/health/**", "/info",
          "/webjars/**", "/v2/api-docs"
        )
          .permitAll().anyRequest().authenticated()
      }
      .oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
  }
}

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
open class MethodSecurityConfiguration : GlobalMethodSecurityConfiguration()

open class AuthAwareTokenConverter : Converter<Jwt, AbstractAuthenticationToken> {
  private val jwtGrantedAuthoritiesConverter: Converter<Jwt, Collection<GrantedAuthority>> =
    JwtGrantedAuthoritiesConverter()

  override fun convert(jwt: Jwt): AbstractAuthenticationToken? =
    AuthAwareAuthenticationToken(jwt, extractAuthorities(jwt))

  @Suppress("UNCHECKED_CAST", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
    val authorities = jwtGrantedAuthoritiesConverter.convert(jwt).toMutableSet()
    if (jwt.claims.containsKey("authorities")) {
      authorities.addAll(
        (jwt.claims["authorities"] as Collection<String?>)
          .map { SimpleGrantedAuthority(it) }.toSet()
      )
    }
    return authorities.toSet()
  }
}

internal class AuthAwareAuthenticationToken(jwt: Jwt, authorities: Collection<GrantedAuthority>) :
  JwtAuthenticationToken(jwt, authorities) {
  override fun getPrincipal(): Any = name
}
