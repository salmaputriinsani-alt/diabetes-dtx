package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LocalizedCopy
import com.example.data.model.AppLanguage
import com.example.data.model.AurelixTab
import com.example.data.model.DiagnosisTrack
import com.example.ui.theme.BorderBeige
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.DeepInk
import com.example.ui.theme.DeepSage
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftSageBg
import com.example.ui.theme.SoftStone
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaSoft
import com.example.ui.theme.WarmAmberBg
import com.example.ui.theme.WarmCream

@Composable
fun AurelixTopBar(
    activeLanguage: AppLanguage,
    activeTrack: DiagnosisTrack,
    stakeholderMode: Boolean,
    copy: LocalizedCopy,
    onLanguageSelected: (AppLanguage) -> Unit,
    onToggleTrack: () -> Unit,
    onToggleStakeholder: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = WarmCream,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(DeepSage)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AURELIX AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Trilingual Language Switcher (MS | EN | ZH)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CrispWhite,
                        border = BorderStroke(1.dp, BorderBeige)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                val selected = lang == activeLanguage
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selected) DeepSage else CrispWhite,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onLanguageSelected(lang) }
                                        .testTag("lang_chip_${lang.code}")
                                ) {
                                    Text(
                                        text = lang.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) CrispWhite else MutedSlate,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            tint = MutedSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Demo Profile Switcher & Stakeholder Ecosystem Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SoftSageBg,
                    border = BorderStroke(1.dp, DeepSage.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleTrack() }
                        .testTag("demo_profile_switcher")
                ) {
                    Text(
                        text = if (activeTrack == DiagnosisTrack.EARLY_T2D_REVERSAL) {
                            "Twin: Encik Ahmad (Early T2D)"
                        } else {
                            "Twin: Aisyah (T1D Preview)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepSage,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (stakeholderMode) TerracottaSoft else CrispWhite,
                    border = BorderStroke(
                        1.dp,
                        if (stakeholderMode) Terracotta else BorderBeige
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleStakeholder() }
                        .testTag("stakeholder_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Ecosystem Stakeholder Mode",
                            tint = if (stakeholderMode) Terracotta else MutedSlate,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (stakeholderMode) "HOTE · B40" else "Ecosystem",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (stakeholderMode) Terracotta else MutedSlate,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (stakeholderMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarmAmberBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = copy.stakeholderBanner,
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepInk,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AurelixBottomNavBar(
    selectedTab: AurelixTab,
    copy: LocalizedCopy,
    onSelectTab: (AurelixTab) -> Unit
) {
    Surface(
        color = CrispWhite,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, BorderBeige),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                label = copy.navHome,
                selected = selectedTab == AurelixTab.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                tag = "nav_home",
                onClick = { onSelectTab(AurelixTab.HOME) },
                modifier = Modifier.weight(1f)
            )
            NavTabItem(
                label = copy.navLog,
                selected = selectedTab == AurelixTab.LOG,
                selectedIcon = Icons.AutoMirrored.Filled.ListAlt,
                unselectedIcon = Icons.AutoMirrored.Outlined.ListAlt,
                tag = "nav_log",
                onClick = { onSelectTab(AurelixTab.LOG) },
                modifier = Modifier.weight(1f)
            )

            // Elevated Terracotta Center Action Button (Makan / Scan Meal)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1.1f)
                    .offset(y = (-12).dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectTab(AurelixTab.SCAN) }
                    .testTag("nav_scan_makan")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (selectedTab == AurelixTab.SCAN) Terracotta else Terracotta.copy(alpha = 0.94f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = copy.navScan,
                        tint = CrispWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = copy.navScan,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Terracotta,
                    maxLines = 1
                )
            }

            NavTabItem(
                label = copy.navDoctor,
                selected = selectedTab == AurelixTab.DOCTOR,
                selectedIcon = Icons.Filled.LocalHospital,
                unselectedIcon = Icons.Outlined.LocalHospital,
                tag = "nav_doctor",
                onClick = { onSelectTab(AurelixTab.DOCTOR) },
                modifier = Modifier.weight(1f)
            )
            NavTabItem(
                label = copy.navProfile,
                selected = selectedTab == AurelixTab.PROFILE,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                tag = "nav_profile",
                onClick = { onSelectTab(AurelixTab.PROFILE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) SoftSageBg else CrispWhite)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) DeepSage else MutedSlate,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) DeepSage else MutedSlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
