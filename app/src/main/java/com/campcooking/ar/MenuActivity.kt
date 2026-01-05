package com.campcooking.ar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campcooking.ar.data.TeamInfo
import com.campcooking.ar.databinding.ActivityMenuBinding
import com.campcooking.ar.utils.DataSubmitManager
import com.campcooking.ar.utils.ServerConfigManager
import com.campcooking.ar.utils.TeamInfoManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 菜单页面Activity
 * 用于输入和保存小组的菜单信息（汤和菜）
 */
class MenuActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMenuBinding
    private lateinit var teamInfoManager: TeamInfoManager
    private lateinit var dataSubmitManager: DataSubmitManager
    private lateinit var serverConfig: ServerConfigManager
    
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // 存储动态生成的菜输入框
    private val dishInputs = mutableListOf<TextInputEditText>()
    private val dishInputLayouts = mutableListOf<TextInputLayout>()
    
    // 最小菜数量
    private val MIN_DISH_COUNT = 4
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "MenuActivity onCreate 开始")
            
            // 保持全屏模式
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            
            Log.d(TAG, "开始加载布局")
            binding = ActivityMenuBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(TAG, "布局加载完成")
            
            teamInfoManager = TeamInfoManager(this)
            dataSubmitManager = DataSubmitManager(this)
            serverConfig = ServerConfigManager(this)
            Log.d(TAG, "管理器初始化完成")
            
            // 获取团队信息
            val teamName = intent.getStringExtra("teamName") ?: "野炊小组"
            binding.teamNameText.text = teamName
            Log.d(TAG, "团队名称: $teamName")
            
            // 初始化菜输入框（至少4个）
            Log.d(TAG, "开始初始化菜输入框")
            for (i in 0 until MIN_DISH_COUNT) {
                addDishInput()
            }
            Log.d(TAG, "菜输入框初始化完成，共 ${dishInputs.size} 个")
            
            setupListeners()
            loadSavedMenu()
            
            Log.d(TAG, "MenuActivity onCreate 完成")
        } catch (e: Exception) {
            Log.e(TAG, "MenuActivity onCreate 出错: ${e.message}", e)
            Toast.makeText(this, "页面加载失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    /**
     * 设置点击事件
     */
    private fun setupListeners() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // 添加菜按钮
        binding.addDishButton.setOnClickListener {
            addDishInput()
        }
        
        // 保存按钮
        binding.saveButton.setOnClickListener {
            saveMenu()
        }
    }
    
    /**
     * 添加一个菜输入框
     */
    private fun addDishInput() {
        val inputLayout = TextInputLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            hint = "菜 ${dishInputs.size + 1}"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(12f, 12f, 12f, 12f)
        }
        
        val inputEditText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 18f
            setPadding(16, 16, 16, 16)
        }
        
        inputLayout.addView(inputEditText)
        binding.dishContainer.addView(inputLayout)
        
        dishInputs.add(inputEditText)
        dishInputLayouts.add(inputLayout)
    }
    
    /**
     * 删除一个菜输入框
     */
    private fun removeDishInput(index: Int) {
        if (dishInputs.size > MIN_DISH_COUNT && index < dishInputs.size) {
            binding.dishContainer.removeView(dishInputLayouts[index])
            dishInputs.removeAt(index)
            dishInputLayouts.removeAt(index)
            
            // 更新标签
            for (i in dishInputLayouts.indices) {
                dishInputLayouts[i].hint = "菜 ${i + 1}"
            }
        }
    }
    
    /**
     * 保存菜单
     */
    private fun saveMenu() {
        // 获取团队信息
        val teamInfo = teamInfoManager.loadTeamInfo()
        if (teamInfo == null || !teamInfo.isValid()) {
            Toast.makeText(this, "请先填写团队信息", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 获取汤
        val soup = binding.soupInput.text?.toString()?.trim() ?: ""
        
        // 获取所有菜（过滤空值）
        val dishes = dishInputs.mapNotNull { it.text?.toString()?.trim() }
            .filter { it.isNotEmpty() }
        
        // 验证至少有一个菜
        if (dishes.isEmpty()) {
            Toast.makeText(this, "请至少输入一个菜", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 构建菜单数据
        val menuData: Map<String, Any> = mapOf(
            "soup" to soup,
            "dishes" to dishes
        )
        
        // 显示保存中提示
        binding.saveButton.isEnabled = false
        binding.saveButton.text = "保存中..."
        
        Log.d(TAG, "开始保存菜单，汤: $soup, 菜数量: ${dishes.size}")
        
        // 在后台线程提交数据
        Thread {
            try {
                // 构建完整数据包
                val dataPackage: Map<String, Any> = mapOf(
                    "teamInfo" to mapOf<String, Any>(
                        "school" to teamInfo.school,
                        "grade" to teamInfo.grade,
                        "className" to teamInfo.className,
                        "stoveNumber" to teamInfo.stoveNumber,
                        "memberCount" to teamInfo.memberCount,
                        "memberNames" to teamInfo.memberNames
                    ),
                    "menuData" to menuData,
                    "exportTime" to System.currentTimeMillis()
                )
                
                // 转换为JSON
                val json = gson.toJson(dataPackage)
                val requestBody = json.toRequestBody("application/json".toMediaType())
                
                // 构建请求
                val serverUrl = serverConfig.getServerUrl()
                val request = Request.Builder()
                    .url("$serverUrl/api/submit_menu")
                    .post(requestBody)
                    .build()
                
                Log.d(TAG, "提交菜单数据到: $serverUrl/api/submit_menu")
                Log.d(TAG, "菜单数据: $json")
                Log.d(TAG, "当前Activity状态: isFinishing=${this@MenuActivity.isFinishing}, isDestroyed=${this@MenuActivity.isDestroyed}")
                
                // 发送请求
                val response = client.newCall(request).execute()
                
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d(TAG, "提交成功: $responseBody")
                        
                        // 保存到本地
                        saveMenuToLocal(menuData)
                        
                        // 检查Activity是否还存在
                        if (!isFinishing && !isDestroyed) {
                            runOnUiThread {
                                binding.saveButton.isEnabled = true
                                binding.saveButton.text = "保存"
                                Toast.makeText(this@MenuActivity, "✅ 菜单已保存并上传", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.w(TAG, "Activity已销毁，无法更新UI")
                        }
                    } else {
                        val errorBody = response.body?.string()
                        val errorMsg = "服务器错误: ${response.code}"
                        Log.e(TAG, "$errorMsg, 响应: $errorBody")
                        
                        // 即使服务器失败，也保存到本地
                        saveMenuToLocal(menuData)
                        
                        // 检查Activity是否还存在
                        if (!isFinishing && !isDestroyed) {
                            runOnUiThread {
                                binding.saveButton.isEnabled = true
                                binding.saveButton.text = "保存"
                                Toast.makeText(this@MenuActivity, "保存失败: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.w(TAG, "Activity已销毁，无法更新UI")
                        }
                    }
                } finally {
                    // 确保关闭响应
                    response.close()
                }
                
            } catch (e: IOException) {
                Log.e(TAG, "网络错误: ${e.message}", e)
                // 保存到本地
                saveMenuToLocal(menuData)
                // 检查Activity是否还存在
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "保存"
                        Toast.makeText(this@MenuActivity, "⚠️ 网络错误，已保存到本地\n请检查网络连接后重试", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "Activity已销毁，无法更新UI")
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存菜单失败: ${e.message}", e)
                // 尝试保存到本地
                try {
                    saveMenuToLocal(menuData)
                } catch (saveError: Exception) {
                    Log.e(TAG, "保存到本地也失败: ${saveError.message}", saveError)
                }
                // 检查Activity是否还存在
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "保存"
                        Toast.makeText(this@MenuActivity, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "Activity已销毁，无法更新UI")
                }
            }
        }.start()
    }
    
    /**
     * 保存菜单到本地SharedPreferences
     */
    private fun saveMenuToLocal(menuData: Map<String, Any>) {
        val prefs = getSharedPreferences("menu_data", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("soup", menuData["soup"] as? String ?: "")
        editor.putString("dishes", gson.toJson(menuData["dishes"]))
        editor.apply()
    }
    
    /**
     * 从本地加载已保存的菜单
     */
    private fun loadSavedMenu() {
        val prefs = getSharedPreferences("menu_data", MODE_PRIVATE)
        val soup = prefs.getString("soup", "") ?: ""
        val dishesJson = prefs.getString("dishes", "[]") ?: "[]"
        
        if (soup.isNotEmpty() || dishesJson != "[]") {
            binding.soupInput.setText(soup)
            
            try {
                val dishes = gson.fromJson(dishesJson, Array<String>::class.java).toList()
                // 确保有足够的输入框
                while (dishInputs.size < dishes.size) {
                    addDishInput()
                }
                // 填充数据
                for (i in dishes.indices) {
                    if (i < dishInputs.size) {
                        dishInputs[i].setText(dishes[i])
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载菜单失败: ${e.message}", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "MenuActivity"
    }
}

