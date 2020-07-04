# Cordova Plugin Arcface
This plugin is base on arcsoft sdk 2.2,and only support android platform.
## Configure SDK
modify config.xml

```xml
    <preference name="AppId" value="value" />
    <preference name="SdkKey" value="value" />
    <preference name="HorizontalOffset" value="0" />
    <preference name="VerticalOffset" value="0" />
```

## Method
### activeEngine
active engine online.

```javascript
activeEngine(resolve,reject)
```
### getFaceFeature
get base64 string of face feature.

```javascript
getFaceFeature(filePath,resolve,reject)
```
### compareFeature
compare two face feature string.

```javascript
compareFeature(featureArray,resolve,reject)
```
