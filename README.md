# 空间魂斗罗 / Spatial Contra

PICO Spatial 原生立体横版闯关游戏，支持左右手柄。包含十个逐步提高难度的关卡、SGS 生成的三维角色和环境、骨骼动作及原创 FC 风格音效。

左右 HUD 分别显示玩家生命、分数、武器和敌人剩余血量，使用相同尺寸和前景位置。操作指南可展开/收起。初始生命为 5，只有实际通关才保存下一关进度。

| 操作 | 功能 |
|---|---|
| 左摇杆 | 移动、瞄准、下推蹲伏 |
| A | 跳跃、菜单确认 |
| B / 右扳机 | 射击 |
| X | 切换已拾取武器 |
| 右摇杆下推 | 趴下，松开站起 |
| 指针点击菜单 | 新游戏、继续、重试、下一关 |

使用 Kotlin、Spatial SDK BOM 6.1.9、原生 SpatialUI 和已预打包的 Editor Bundle。模型和全景为本项目素材，音效为原创合成；未使用原作 APK、ROM 或解包素材。游戏参考经典横版射击规则。

构建需要 JDK 21、Android SDK 35 和项目 Gradle Wrapper：

```powershell
.\gradlew.bat assembleRelease
.\gradlew.bat testDebugUnitTest assembleDebugAndroidTest
```

场景资源随源码提交，构建不启动 Spatial Editor 或 Node。单元测试 35 项通过；模拟器原生集成通过，覆盖角色骨骼、显隐、趴下、十关环境替换、敌人血量和音效加载。真机文字与布局已由用户确认。模拟器输入不代表实体手柄方向、性能和佩戴舒适度测试。

关卡包括丛林、断桥、地堡、瀑布、雪地、能源基地、机库、熔炉、实验室和异形据点。首次游戏从第一关零分开始，存档只在真实通关后推进。
