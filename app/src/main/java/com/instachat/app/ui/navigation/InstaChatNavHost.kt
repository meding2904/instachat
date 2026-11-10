package com.instachat.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.instachat.app.ui.chat.ChatScreen
import com.instachat.app.ui.contact.ContactEditorScreen
import com.instachat.app.ui.inbox.InboxScreen
import com.instachat.app.ui.script.ScriptEditorScreen
import com.instachat.app.ui.settings.SettingsScreen
import com.instachat.app.ui.themepicker.ThemePickerScreen

object Routes {
    const val INBOX = "inbox"
    const val SETTINGS = "settings"
    const val CONTACT_EDITOR = "contact_editor/{contactId}"
    const val CHAT = "chat/{contactId}?presentation={presentation}"
    const val THEME_PICKER = "theme/{contactId}"
    const val SCRIPT_EDITOR = "script/{contactId}"

    fun chat(contactId: Long, presentation: Boolean = false): String =
        "chat/$contactId?presentation=$presentation"

    fun contactEditor(contactId: Long = -1L): String = "contact_editor/$contactId"

    fun themePicker(contactId: Long): String = "theme/$contactId"

    fun scriptEditor(contactId: Long): String = "script/$contactId"
}

@Composable
fun InstaChatNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.INBOX,
        enterTransition = {
            fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 }
        },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = {
            fadeIn(tween(200)) + slideInHorizontally(tween(240)) { -it / 18 }
        },
        popExitTransition = {
            fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { it / 12 }
        }
    ) {
        composable(Routes.INBOX) {
            InboxScreen(
                onOpenChat = { navController.navigate(Routes.chat(it)) },
                onNewContact = { navController.navigate(Routes.contactEditor()) },
                onEditContact = { navController.navigate(Routes.contactEditor(it)) },
                onOpenScripts = { navController.navigate(Routes.scriptEditor(it)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("contactId") { type = NavType.LongType },
                navArgument("presentation") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            ChatScreen(
                onBack = { navController.popBackStack() },
                onEditContact = { navController.navigate(Routes.contactEditor(it)) },
                onOpenTheme = { navController.navigate(Routes.themePicker(it)) },
                onOpenScripts = { navController.navigate(Routes.scriptEditor(it)) },
                onOpenPresentation = {
                    navController.navigate(Routes.chat(it, presentation = true))
                }
            )
        }

        composable(
            route = Routes.CONTACT_EDITOR,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) {
            ContactEditorScreen(
                onBack = { navController.popBackStack() },
                onOpenTheme = { navController.navigate(Routes.themePicker(it)) },
                onFinished = { contactId, isNew ->
                    navController.popBackStack()
                    if (isNew) {
                        navController.navigate(Routes.chat(contactId))
                    }
                },
                onDeleted = {
                    navController.popBackStack(Routes.INBOX, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.THEME_PICKER,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) {
            ThemePickerScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.SCRIPT_EDITOR,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) {
            ScriptEditorScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
