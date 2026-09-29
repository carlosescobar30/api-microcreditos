package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.common.exception.conflict.EmailConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.UsernameConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.iam.domain.Role;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RoleName;
import com.carlosescobar30.apimicrocreditos.iam.dto.RegisterRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.repository.UserRepository;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.service.contract.UserScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 42L;
    private static final String USERNAME = "carlos";
    private static final String EMAIL = "carlos@mail.com";
    private static final String RAW_PASSWORD = "plain-password";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleService roleService;
    @Mock
    private UserScoreService userScoreService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        this.userService = new UserService(repository, passwordEncoder, roleService, userScoreService);
    }

    @Nested
    @DisplayName("Tests for the createUser method")
    class CreateUserTests {

        @Test
        void nothingIsSavedNorEncodedWhenTheUsernameIsAlreadyTaken() {

            when(repository.existsByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(registerRequest()))
                    .isInstanceOf(UsernameConflictException.class);

            verify(repository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void nothingIsSavedNorEncodedWhenTheEmailIsAlreadyTaken() {

            when(repository.existsByUsername(USERNAME)).thenReturn(false);
            when(repository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(registerRequest()))
                    .isInstanceOf(EmailConflictException.class);

            verify(repository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void theStoredPasswordIsTheEncodedOneNeverThePlainOne() {

            givenAnAvailableUsernameAndEmail();

            userService.createUser(registerRequest());

            assertThat(savedUser().getPasswordHash())
                    .isEqualTo(ENCODED_PASSWORD)
                    .isNotEqualTo(RAW_PASSWORD);
        }

        @Test
        void theUserIsCreatedWithTheDefaultRoleOnly() {

            givenAnAvailableUsernameAndEmail();

            userService.createUser(registerRequest());

            assertThat(savedUser().getRoles())
                    .extracting(Role::getName)
                    .containsExactly(RoleName.ROLE_USER);
        }

        @Test
        void theUserIsCreatedWithoutIdentityVerification() {

            givenAnAvailableUsernameAndEmail();

            userService.createUser(registerRequest());

            assertThat(savedUser().getIsIdentityVerified()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for the getUserDetailImpl method")
    class GetUserDetailImplTests {

        @Test
        void aResourceNotFoundExceptionIsThrownWhenTheUserDoesNotExist() {

            when(repository.findWithRolesById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserDetailImpl(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void theRolesAreMappedToAuthoritiesKeepingTheRolePrefix() {

            when(repository.findWithRolesById(USER_ID)).thenReturn(Optional.of(existingUser()));

            UserDetailsImpl userDetails = userService.getUserDetailImpl(USER_ID);

            assertThat(userDetails.getId()).isEqualTo(USER_ID);
            assertThat(userDetails.getUsername()).isEqualTo(USERNAME);
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("Tests for the verifyIdentity method")
    class VerifyIdentityTests {

        @Test
        void theUserIsMarkedAsVerified() {

            User user = existingUser();
            when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            userService.verifyIdentity(USERNAME);

            assertThat(user.getIsIdentityVerified()).isTrue();
        }

        @Test
        void aResourceNotFoundExceptionIsThrownWhenTheUserDoesNotExist() {

            when(repository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.verifyIdentity(USERNAME))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests for the createAdminIfMissing method")
    class CreateAdminIfMissingTests {

        @Test
        void theAdminIsCreatedVerifiedWithTheAdminRoleAndAnEncodedPassword() {

            when(repository.existsByUsername(USERNAME)).thenReturn(false);
            when(repository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(roleService.getRole(RoleName.ROLE_ADMIN)).thenReturn(new Role(2L, RoleName.ROLE_ADMIN));
            when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

            userService.createAdminIfMissing(USERNAME, EMAIL, RAW_PASSWORD);

            User admin = savedUser();
            assertThat(admin.getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_ADMIN);
            assertThat(admin.getIsIdentityVerified()).isTrue();
            assertThat(admin.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        void nothingIsCreatedWhenTheUsernameIsAlreadyTaken() {

            when(repository.existsByUsername(USERNAME)).thenReturn(true);

            userService.createAdminIfMissing(USERNAME, EMAIL, RAW_PASSWORD);

            verify(repository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void nothingIsCreatedWhenTheEmailIsAlreadyTaken() {

            when(repository.existsByUsername(USERNAME)).thenReturn(false);
            when(repository.existsByEmail(EMAIL)).thenReturn(true);

            userService.createAdminIfMissing(USERNAME, EMAIL, RAW_PASSWORD);

            verify(repository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
        }
    }

    private void givenAnAvailableUsernameAndEmail() {

        when(repository.existsByUsername(USERNAME)).thenReturn(false);
        when(repository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(roleService.getRole(RoleName.ROLE_USER)).thenReturn(new Role(1L, RoleName.ROLE_USER));
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    private User savedUser() {

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private RegisterRequestDTO registerRequest() {

        return new RegisterRequestDTO(
                "Carlos",
                "Escobar",
                USERNAME,
                RAW_PASSWORD,
                EMAIL,
                LocalDate.of(1990, 1, 1)
        );
    }

    private User existingUser() {

        Set<Role> roles = new HashSet<>();
        roles.add(new Role(1L, RoleName.ROLE_USER));

        User user = User.builder()
                .name("Carlos")
                .lastName("Escobar")
                .username(USERNAME)
                .passwordHash(ENCODED_PASSWORD)
                .email(EMAIL)
                .roles(roles)
                .isIdentityVerified(false)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        user.setId(USER_ID);
        return user;
    }
}
