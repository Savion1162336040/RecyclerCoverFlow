
# RecyclerCoverFlow
本库从[RecyclerCoverFlow](https://github.com/ChenLittlePing/RecyclerCoverFlow)修改而来
<br>
使用RecyclerView，自定义LayoutManager实现旋转木马相册效果

## 添加依赖项
[![](https://jitpack.io/v/Savion1162336040/RecyclerCoverFlow.svg)](https://jitpack.io/#Savion1162336040/RecyclerCoverFlow)

### 将jitpack添加到您的项目根目录下的build.gradle文件
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
### 将此依赖项添加到您的应用级build.gradle文件里
```
dependencies {
    implementation 'com.github.Savion1162336040:RecyclerCoverFlow:1.0'
}
```

<br>并新增了如下特性
- 无限循环滚动时可指定smoothScrollToPosition与scrollToPosition
- 支持水平与垂直两种展示模式

![image](https://github.com/ChenLittlePing/RecyclerCoverFlow/blob/master/gif/demo.gif)

具体使用方法与[RecyclerCoverFlow](https://github.com/ChenLittlePing/RecyclerCoverFlow)完全相同
