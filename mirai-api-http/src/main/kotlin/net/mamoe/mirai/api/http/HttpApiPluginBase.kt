/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.api.http

import com.google.auto.service.AutoService
import kotlinx.coroutines.async
import net.mamoe.mirai.api.http.command.HttpApiCommandOwner
import net.mamoe.mirai.api.http.config.Setting
import net.mamoe.mirai.api.http.service.MiraiApiHttpServices
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.User
import java.io.File

internal typealias CommandSubscriber = suspend (String, Long, Long, List<String>) -> Unit

@AutoService(JvmPlugin::class)
object HttpApiPluginBase : KotlinPlugin(HttpApiPluginDescription) {
    var services: MiraiApiHttpServices = MiraiApiHttpServices(this);

    override fun onLoad() {
        logger.info("Loading Mirai HTTP API plugin")
    }

    override fun onEnable() {
        Setting.reload()

        with(Setting) {
            logger.info("Trying to Start Mirai HTTP Server in 0.0.0.0:$port")

            if (authKey.startsWith("INITKEY")) {
                logger.warning("USING INITIAL KEY, please edit the key")
            }

            services.onLoad()

            logger.info("Starting Mirai HTTP Server in $host:$port")
            MiraiHttpAPIServer.start(host, port, authKey)

            services.onEnable()
        }
    }

    override fun onDisable() {
        MiraiHttpAPIServer.stop()

        services.onDisable()
    }

    private val subscribers = mutableListOf<CommandSubscriber>()

    internal fun subscribeCommand(subscriber: CommandSubscriber): CommandSubscriber =
        subscriber.also { subscribers.add(it) }

    internal fun unSubscribeCommand(subscriber: CommandSubscriber) = subscribers.remove(subscriber)

    // TODO: 解决Http-api插件卸载后，注册的command将失效
    internal fun registerCommand(
        names: Array<out String>,
        description: String,
        usage: String,
    ) {
        CommandManager.INSTANCE.run {
            object : SimpleCommand(HttpApiCommandOwner, *names, description = description) {
                override val usage: String = usage

                @Handler
                suspend fun onCommand(target: User, message: String) {
                    // TODO
                }
            }
        }

        /* registerCommand {
        this.name = name
        this.alias = alias
        this.description = description
        this.usage = usage

        this.onCommand {
                // do nothing
                true
            }
        }*/
    }

//    override suspend fun onCommand(command: Command, sender: CommandSender, args: List<String>) {
//        launch {
//            val (from: Long, group: Long) = when (sender) {
//                is MemberCommandSender -> sender.user.id to sender.user.id
//                is FriendCommandSender -> sender.user.id to 0L
//                else -> 0L to 0L // 考虑保留对其他Sender类型的扩展，先统一默认为ConsoleSender
//            }
//
//            subscribers.forEach {
//                it(command.names.first(), from, group, args)
//            }
//        }
//    }

    private val imageFold: File = File(dataFolder, "images").apply { mkdirs() }

    internal fun image(imageName: String) = File(imageFold, imageName)

    fun saveImageAsync(name: String, data: ByteArray) =
        async {
            image(name).apply { writeBytes(data) }
        }

    private val voiceFold: File = File(dataFolder, "voices").apply { mkdirs() }

    internal fun voice(voiceName: String) = File(voiceFold, voiceName)

    fun saveVoiceAsync(name: String, data: ByteArray) =
        async {
            voice(name).apply { writeBytes(data) }
        }

}
