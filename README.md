# SquareLayoutManager
### 效果图

<img width="300" height="600" src="https://github.com/sinyu1012/SquareLayoutManager/blob/main/image/demo.gif" />



### 添加依赖
Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```


Add the dependency

```groovy
dependencies {
      implementation 'io.iftech.android:SquareLayoutManager:<version>'
}
```

### 实现原理

[小宇宙广场列表实现SquareLayoutManager](https://zhuanlan.zhihu.com/p/434281758)

### 基本使用

设置 `RecyclerView` 的 `LayoutManager`：

```kotlin
// 构造函数参数 spanCount为一行有多少个 ItemView，startPosition 为起始位置（不传默认中间）
rvSquare.layoutManager = SquareLayoutManager(20, 10)
```

滑动到指定位置：

```kotlin
layoutManager.smoothScrollToPosition(position) 
```

是否自动居中：

```kotlin
// 类似 LinearSnapHelper 效果，使用 fling 实现，默认为true
layoutManager.isAutoSelect = true
```

初始化布局时，是否定位到中心的 item：

```kotlin
// 默认为 true
layoutManager.isInitLayoutCenter = true
```

设置选中 Item 的监听回调：

```kotlin
layoutManager.setOnItemSelectedListener { postion ->
    Toast.makeText(context, "当前选中：$postion", Toast.LENGTH_SHORT).show()
}
```

滑动到中心：

```kotlin
layoutManager.smoothScrollToCenter()
```

