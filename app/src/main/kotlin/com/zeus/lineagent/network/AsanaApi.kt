package com.zeus.lineagent.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface AsanaApi {

    @GET("api/1.0/projects/{project_gid}/tasks")
    suspend fun listProjectTasks(
        @Header("Authorization") bearer: String,
        @Path("project_gid") projectGid: String,
        @Query("completed_since") completedSince: String = "now",
        @Query("opt_fields") optFields: String =
            "name,due_on,due_at,assignee.name,completed,permalink_url,notes",
        @Query("limit") limit: Int = 50,
    ): AsanaListResponse<AsanaTask>

    @GET("api/1.0/workspaces/{workspace_gid}/projects")
    suspend fun listProjects(
        @Header("Authorization") bearer: String,
        @Path("workspace_gid") workspaceGid: String,
        @Query("opt_fields") optFields: String = "name,archived",
        @Query("archived") archived: Boolean = false,
        @Query("limit") limit: Int = 100,
    ): AsanaListResponse<AsanaProject>

    @GET("api/1.0/users/me")
    suspend fun me(
        @Header("Authorization") bearer: String,
        @Query("opt_fields") optFields: String = "name,email,workspaces.name",
    ): AsanaSingleResponse<AsanaUser>
}

@Serializable
data class AsanaListResponse<T>(val data: List<T> = emptyList())

@Serializable
data class AsanaSingleResponse<T>(val data: T? = null)

@Serializable
data class AsanaTask(
    val gid: String,
    val name: String = "",
    val due_on: String? = null,
    val due_at: String? = null,
    val completed: Boolean = false,
    val permalink_url: String? = null,
    val notes: String? = null,
    val assignee: AsanaAssignee? = null,
)

@Serializable
data class AsanaAssignee(val gid: String? = null, val name: String? = null)

@Serializable
data class AsanaProject(val gid: String, val name: String = "", val archived: Boolean = false)

@Serializable
data class AsanaUser(
    val gid: String,
    val name: String = "",
    val email: String? = null,
    val workspaces: List<AsanaWorkspace> = emptyList(),
)

@Serializable
data class AsanaWorkspace(val gid: String, val name: String = "")
