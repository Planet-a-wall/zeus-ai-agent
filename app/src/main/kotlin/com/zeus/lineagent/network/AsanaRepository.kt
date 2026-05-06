package com.zeus.lineagent.network

class AsanaRepository(private val api: AsanaApi = NetworkModule.asanaApi) {

    suspend fun openTasks(token: String, projectGid: String): Result<List<AsanaTask>> = runCatching {
        require(token.isNotBlank()) { "Asana token is empty" }
        require(projectGid.isNotBlank()) { "Asana project gid is empty" }
        api.listProjectTasks("Bearer $token", projectGid).data
            .filter { !it.completed }
    }

    suspend fun listProjects(token: String, workspaceGid: String): Result<List<AsanaProject>> = runCatching {
        require(token.isNotBlank()) { "Asana token is empty" }
        require(workspaceGid.isNotBlank()) { "Asana workspace gid is empty" }
        api.listProjects("Bearer $token", workspaceGid).data
    }

    suspend fun me(token: String): Result<AsanaUser> = runCatching {
        require(token.isNotBlank()) { "Asana token is empty" }
        api.me("Bearer $token").data ?: error("Asana returned no user")
    }
}
