package com.example.sosapp.ui.screen.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sosapp.R
import com.example.sosapp.data.model.UserData
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.profile.EmergencyContactRow
import com.example.sosapp.ui.components.common.InfoCard
import com.example.sosapp.ui.components.common.PrimaryButton
import com.example.sosapp.ui.components.profile.ProfileInfoRow
import com.example.sosapp.ui.components.common.SectionTitle
import com.example.sosapp.ui.theme.EmergencyColors
import com.example.sosapp.ui.theme.SOSAppTheme
import com.example.sosapp.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCurrentUser()
    }

    val userProfile = currentUser ?: UserData()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmergencyColors.EmergencyBackground),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Header Card
            InfoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Picture Placeholder
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.profilePictureUrl != null) {
                            // TODO: Load actual profile picture when implemented
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            // Show first letter of name or default icon
                            if (userProfile.fullName.isNotBlank()) {
                                BodyText(
                                    text = userProfile.fullName.first().toString().uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // User Name
                    SectionTitle(
                        text = userProfile.fullName.takeIf { it.isNotBlank() } ?: "Guest User",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Verification Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (userProfile.isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (userProfile.isVerified) "Verified" else "Not Verified",
                            tint = if (userProfile.isVerified) EmergencyColors.SafeGreen else EmergencyColors.WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )

                        BodyText(
                            text = if (userProfile.isVerified) "Account Verified" else "Verification Pending",
                            color = if (userProfile.isVerified) EmergencyColors.SafeGreen else EmergencyColors.WarningOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Edit Profile Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Contact Information Card
            InfoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SectionTitle(
                        text = "Contact Information",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )

                    ProfileInfoRow(
                        icon = Icons.Default.Email,
                        label = "Email Address",
                        value = userProfile.email.takeIf { it.isNotBlank() } ?: "Not provided"
                    )
                }
            }

            // Emergency Contacts Card
            InfoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(
                            text = "Emergency Contacts",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp
                        )

                        BodyText(
                            text = "${userProfile.emergencyContacts.size}/3",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (userProfile.emergencyContacts.isNotEmpty()) {
                        userProfile.emergencyContacts.forEachIndexed { index, contact ->
                            EmergencyContactRow(
                                contactNumber = index + 1,
                                phoneNumber = contact
                            )
                        }

                        if (userProfile.emergencyContacts.size < 3) {
                            BodyText(
                                text = "💡 You can add up to ${3 - userProfile.emergencyContacts.size} more emergency contacts",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "No contacts",
                                tint = EmergencyColors.WarningOrange,
                                modifier = Modifier.size(32.dp)
                            )

                            BodyText(
                                text = "No emergency contacts added yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            BodyText(
                                text = "Add at least one emergency contact to ensure you can get help when needed",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            PrimaryButton(
                                text = "Add Emergency Contacts",
                                onClick = onEditProfile,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Account Information Card
            InfoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionTitle(
                        text = "Account Information",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )

                    ProfileInfoRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Account Status",
                        value = if (userProfile.isVerified) "Verified" else "Pending Verification",
                        valueColor = if (userProfile.isVerified)
                            EmergencyColors.SafeGreen
                        else
                            EmergencyColors.WarningOrange
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BodyText(
                                text = "🔒 Security & Privacy",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            BodyText(
                                text = "• Your data is encrypted and secure\n• Emergency contacts are notified instantly\n• Location sharing only during emergencies\n• No personal data is shared with third parties",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Logout Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionTitle(
                        text = "Account Actions",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 18.sp
                    )

                    if (!showLogoutConfirmation) {
                        PrimaryButton(
                            text = "Sign Out",
                            onClick = { showLogoutConfirmation = true },
                            backgroundColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BodyText(
                                text = "Are you sure you want to sign out?",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )

                            BodyText(
                                text = "You'll need to log in again to access emergency features",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PrimaryButton(
                                    text = "Cancel",
                                    onClick = { showLogoutConfirmation = false },
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                PrimaryButton(
                                    text = "Sign Out",
                                    onClick = {
                                        showLogoutConfirmation = false
                                        viewModel.logout()
                                        onLogout()
                                    },
                                    backgroundColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenLightPreview() {
    SOSAppTheme(darkTheme = false) {
        ProfileScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenDarkPreview() {
    SOSAppTheme(darkTheme = true) {
        ProfileScreen()
    }
}