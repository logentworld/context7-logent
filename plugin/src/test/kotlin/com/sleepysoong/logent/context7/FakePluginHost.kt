package com.sleepysoong.logent.context7

import com.sleepysoong.logent.plugin.PluginHost
import com.sleepysoong.logent.plugin.PluginHttpRequest
import com.sleepysoong.logent.plugin.PluginHttpResponse
import com.sleepysoong.logent.plugin.ProcessResult

/** 네트워크와 셸을 직접 부르지 않고 요청·응답을 붙잡아 두는 테스트 호스트예요. */
internal class FakePluginHost(
    override val appVersion: String = "0.0.0-test",
    override val apkPath: String = "/data/app/~~abc/com.sleepysoong.logent-1/base.apk",
    private val values: Map<String, String> = emptyMap(),
    var respond: suspend (PluginHttpRequest) -> Result<PluginHttpResponse> = {
        Result.failure(IllegalStateException("테스트에서 HTTP 응답을 준비하지 않았어요."))
    },
) : PluginHost {
    val requests = mutableListOf<PluginHttpRequest>()

    override suspend fun http(request: PluginHttpRequest): Result<PluginHttpResponse> {
        requests += request
        return respond(request)
    }

    override suspend fun shell(command: String, timeoutSeconds: Int): Result<ProcessResult> =
        Result.success(ProcessResult(0, "", false))

    override suspend fun ensureDesktop(): Result<Unit> = Result.success(Unit)

    override suspend fun contentArchive(assetPrefix: String): Result<String> = Result.success(apkPath)

    override suspend fun readAsset(path: String): Result<ByteArray> =
        Result.failure(IllegalArgumentException("테스트에서는 자산을 읽지 않아요: $path"))

    override fun setting(key: String): String? = values[key]

    override fun progress(message: String) = Unit
}
