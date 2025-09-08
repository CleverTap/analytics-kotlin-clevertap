# CleverTap Analytics Kotlin SDK

CleverTap destination plugin for Segment Analytics Kotlin. 

## Installation

Add the following dependencies to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.clevertap.android:clevertap-android-sdk:7.5.1")
    implementation("com.segment.analytics.kotlin:android:1.21.0")
    implementation("com.clevertap.android:clevertap-segment.kotlin:1.0.0")
}
```

## Quick Start

```kotlin
// Initialize
analytics = Analytics(WRITE_KEY, applicationContext).apply {
    this.add(plugin = CleverTapDestination(applicationContext, ::cleverTapReady))
}
```

## Documentation

- **[Usage Guide](docs/Usage.md)** - Comprehensive guide on how to use the SDK
- **[Sample App](testapp)** - Complete example application demonstrating all features
- **[Changelog](CHANGELOG.md)** - Version history and release notes


## Requirements
- Android API level 21+

## License

```
MIT License

Copyright (c) 2021 Segment

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```