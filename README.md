# 一、題目名稱
沉浸式博物館展品導覽
# 二、專案介紹
本專案旨在開發一個沉浸式的博物館展品導覽系統，提升觀眾的參觀體驗。透過手機設備，觀眾能夠獲取展品的多媒體介紹、3D 模型展示以及說明，讓歷史與藝術更生動、具象。

# 三、主要功能
1. QR Code 掃描：快速識別展品資訊，QR Code內容為展品資源識別資料
2. 3D 模型展示：動態載入並展示展品的 3D 立體模型
3. 介紹展品：提供文字介紹，讓觀眾了解展品的背景與故事。
# 四、專案結構

.. code-block:: text

   ├── app/
   │   ├── src/
   │   │   ├── main/
   │   │   │   ├── java/
   │   │   │   ├── res/
   │   │   │   └── AndroidManifest.xml
   ├── gradle/
   ├── .gitignore
   ├── README.md
   ├── build.gradle
   ├── gradle.properties
   ├── gradlew
   ├── gradlew.bat
   └── settings.gradle
# 五、開發環境
- 開發工具：Android Studio
- 語言：Java
- 建構工具：Gradle
# 六、使用說明
1.克隆此專案：
 .. code-block::
 git clone https://github.com/laiaki/Embedded-Computer-Vision-System-Project.git
2.開啟 Android Studio，選擇「Open an existing Android Studio project」，並選擇專案資料夾。
3.等待 Gradle 同步完成後，連接 Android 裝置或啟動模擬器，點擊「Run」以執行應用程式。
4.掃描qr-code.png
