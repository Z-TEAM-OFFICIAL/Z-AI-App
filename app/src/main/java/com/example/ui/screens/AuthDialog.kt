package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.database.UserAccount
import com.example.ui.theme.*
import com.example.ui.viewmodel.ZegaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    viewModel: ZegaViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isLoading by viewModel.isAuthLoading.collectAsState()

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.clearAuthError()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(CosmicCard)
                .border(1.dp, CosmicCardBorder, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(ZegaNeonCyan, ZegaNeonViolet))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSignUp) Icons.Default.PersonAdd else Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isSignUp) "Create Account" else "Welcome Back",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaWhite
                            )
                            Text(
                                text = "Z-AI Privacy Intelligence",
                                fontSize = 12.sp,
                                color = ZegaGrayText
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearAuthError()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_auth_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ZegaGrayText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Toggle tabs: Sign In vs Sign Up
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CosmicBackground)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isSignUp) ZegaNeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                isSignUp = false
                                localValidationError = null
                                viewModel.clearAuthError()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            fontSize = 14.sp,
                            fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isSignUp) ZegaNeonCyan else ZegaGrayText,
                            modifier = Modifier.testTag("tab_sign_in")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSignUp) ZegaNeonViolet.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable {
                                isSignUp = true
                                localValidationError = null
                                viewModel.clearAuthError()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Account",
                            fontSize = 14.sp,
                            fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSignUp) ZegaNeonViolet else ZegaGrayText,
                            modifier = Modifier.testTag("tab_sign_up")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error Message banner
                val errorMessage = localValidationError ?: authError
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4A1515))
                            .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                modifier = Modifier.testTag("auth_error_text")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Display Name field (Sign Up only)
                if (isSignUp) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            localValidationError = null
                        },
                        label = { Text("Display Name / Nickname", color = ZegaGrayText) },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = ZegaNeonCyan)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ZegaNeonCyan,
                            unfocusedBorderColor = CosmicCardBorder,
                            focusedTextColor = ZegaWhite,
                            unfocusedTextColor = ZegaWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_display_name")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localValidationError = null
                    },
                    label = { Text("Email Address", color = ZegaGrayText) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = ZegaNeonCyan)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZegaNeonCyan,
                        unfocusedBorderColor = CosmicCardBorder,
                        focusedTextColor = ZegaWhite,
                        unfocusedTextColor = ZegaWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localValidationError = null
                    },
                    label = { Text("Password", color = ZegaGrayText) },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = ZegaNeonCyan)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = ZegaGrayText
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!isSignUp) {
                                viewModel.login(email, password)
                            }
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZegaNeonCyan,
                        unfocusedBorderColor = CosmicCardBorder,
                        focusedTextColor = ZegaWhite,
                        unfocusedTextColor = ZegaWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password")
                )

                // Confirm Password field (Sign Up only)
                if (isSignUp) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            localValidationError = null
                        },
                        label = { Text("Confirm Password", color = ZegaGrayText) },
                        leadingIcon = {
                            Icon(Icons.Default.LockClock, contentDescription = null, tint = ZegaNeonViolet)
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ZegaNeonViolet,
                            unfocusedBorderColor = CosmicCardBorder,
                            focusedTextColor = ZegaWhite,
                            unfocusedTextColor = ZegaWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_password")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.isBlank()) {
                            localValidationError = "Please enter an email."
                            return@Button
                        }
                        if (password.length < 6) {
                            localValidationError = "Password must be at least 6 characters."
                            return@Button
                        }
                        if (isSignUp && password != confirmPassword) {
                            localValidationError = "Passwords do not match."
                            return@Button
                        }

                        if (isSignUp) {
                            viewModel.signUp(email, password, displayName) { success ->
                                if (success) onDismiss()
                            }
                        } else {
                            viewModel.login(email, password) { success ->
                                if (success) onDismiss()
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSignUp) ZegaNeonViolet else ZegaNeonCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_auth_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUp) "Create Account" else "Sign In",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSignUp) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Divider: OR Continue With
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CosmicCardBorder)
                    Text(
                        text = "  OR SOCIAL SIGN-IN  ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaGrayText,
                        letterSpacing = 1.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CosmicCardBorder)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Social Sign-In Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Sign-In button
                    OutlinedButton(
                        onClick = {
                            viewModel.loginWithSocial(
                                provider = "GOOGLE",
                                email = "google.user@example.com",
                                displayName = "Google User",
                                avatarColor = "#4285F4"
                            ) { success ->
                                if (success) onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF4285F4).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("google_signin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google Sign In",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZegaWhite
                        )
                    }

                    // GitHub Sign-In button
                    OutlinedButton(
                        onClick = {
                            viewModel.loginWithSocial(
                                provider = "GITHUB",
                                email = "github.dev@example.com",
                                displayName = "GitHub Developer",
                                avatarColor = "#9D00FF"
                            ) { success ->
                                if (success) onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ZegaNeonViolet.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ZegaNeonViolet.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("github_signin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "GitHub Sign In",
                            tint = ZegaNeonViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GitHub",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZegaWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Guest / Incognito Option
                TextButton(
                    onClick = {
                        viewModel.loginWithSocial(
                            provider = "GUEST",
                            email = "guest.local@z-ai.local",
                            displayName = "Guest Explorer",
                            avatarColor = "#00FFCC"
                        ) { success ->
                            if (success) onDismiss()
                        }
                    },
                    modifier = Modifier.testTag("guest_signin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = ZegaPrivacyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Continue as Guest (100% Offline Mode)",
                        fontSize = 12.sp,
                        color = ZegaPrivacyGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun UserProfileDialog(
    viewModel: ZegaViewModel,
    user: UserAccount,
    onDismiss: () -> Unit,
    onOpenAuth: () -> Unit
) {
    val allUsersList by viewModel.allUsers.collectAsState(initial = emptyList<UserAccount>())
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(CosmicCard)
                .border(1.dp, CosmicCardBorder, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top close bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaWhite
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ZegaGrayText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Avatar Icon with Glowing ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(ZegaNeonCyan, ZegaNeonViolet, ZegaPrivacyGreen, ZegaNeonCyan)
                            )
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(CosmicBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.take(2).uppercase(Locale.getDefault()),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaNeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZegaWhite,
                    modifier = Modifier.testTag("profile_display_name")
                )

                Text(
                    text = user.email,
                    fontSize = 13.sp,
                    color = ZegaGrayText,
                    modifier = Modifier.testTag("profile_email")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Provider Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (user.provider) {
                                "GOOGLE" -> Color(0xFF4285F4).copy(alpha = 0.2f)
                                "GITHUB" -> ZegaNeonViolet.copy(alpha = 0.25f)
                                "GUEST" -> ZegaPrivacyGreen.copy(alpha = 0.2f)
                                else -> ZegaNeonCyan.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (user.provider) {
                            "GOOGLE" -> "Google Account"
                            "GITHUB" -> "GitHub Account"
                            "GUEST" -> "Offline Guest"
                            else -> "Verified Email Account"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (user.provider) {
                            "GOOGLE" -> Color(0xFF8AB4F8)
                            "GITHUB" -> ZegaNeonViolet
                            "GUEST" -> ZegaPrivacyGreen
                            else -> ZegaNeonCyan
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metadata cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CosmicBackground)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MEMBER SINCE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaGrayText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateFormat.format(Date(user.createdAt)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZegaWhite
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CosmicBackground)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "DATA PRIVACY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaGrayText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "100% On-Device",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZegaPrivacyGreen
                            )
                        }
                    }
                }

                // Switch Account section if multiple exist
                if (allUsersList.size > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SWITCH ACCOUNT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaGrayText,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allUsersList.filter { it.id != user.id }.forEach { otherUser ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicBackground)
                                    .clickable {
                                        viewModel.switchAccount(otherUser)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(ZegaNeonViolet.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = otherUser.displayName.take(1).uppercase(Locale.getDefault()),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZegaWhite
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = otherUser.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ZegaWhite
                                        )
                                        Text(
                                            text = otherUser.email,
                                            fontSize = 11.sp,
                                            color = ZegaGrayText
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch",
                                    tint = ZegaNeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Switch/Add Account & Sign Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenAuth()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CosmicCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("add_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = ZegaNeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Account",
                            fontSize = 12.sp,
                            color = ZegaWhite
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.logout()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935).copy(alpha = 0.2f),
                            contentColor = Color(0xFFFF8A80)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log Out",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Log Out",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8A80)
                        )
                    }
                }
            }
        }
    }
}
