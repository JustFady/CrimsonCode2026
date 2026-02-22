package org.crimsoncode2026.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import Icons.Filled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.UserContact
import org.crimsoncode2026.utils.PhoneNumberUtils

/**
 * Contact list item composable for displaying contacts in selection list.
 *
 * Displays:
 * - Avatar placeholder with initials
 * - Contact display name
 * - Masked phone number (+1 (***) ***-XXXX)
 * - App user badge (shown when hasApp = true)
 * - Selection indicator (when isSelected = true)
 *
 * Used in contact selection screen for multi-select functionality.
 *
 * @param contact The contact to display
 * @param isSelected Whether this contact is currently selected
 * @param onClick Callback when item is clicked
 * @param modifier Modifier for the list item
 */
@Composable
fun ContactListItem(
    contact: UserContact,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar placeholder with initials
            AvatarPlaceholder(
                displayName = contact.displayName,
                modifier = Modifier.size(48.dp)
            )

            // Contact info (name + masked phone + app badge)
            ContactInfo(
                displayName = contact.displayName,
                phoneNumber = contact.contactPhoneNumber,
                hasApp = contact.hasApp,
                modifier = Modifier.weight(1f)
            )

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Avatar placeholder displaying initials from display name.
 *
 * @param displayName The contact's display name
 * @param modifier Modifier for the avatar
 */
@Composable
private fun AvatarPlaceholder(
    displayName: String,
    modifier: Modifier = Modifier
) {
    val initials = displayName
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifBlank { "?" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Contact information display with name, masked phone, and app badge.
 *
 * @param displayName The contact's display name
 * @param phoneNumber The normalized E.164 phone number
 * @param hasApp Whether the contact has the app installed
 * @param modifier Modifier for the info container
 */
@Composable
private fun ContactInfo(
    displayName: String,
    phoneNumber: String,
    hasApp: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = PhoneNumberUtils.maskPhoneNumber(phoneNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // App user badge
        if (hasApp) {
            AppUserBadge()
        }
    }
}

/**
 * Badge indicating the contact has the app installed.
 *
 * Shows as a small pill/badge with "App" label.
 */
@Composable
private fun AppUserBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(
            text = "App",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
