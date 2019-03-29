# react-native-stripe-terminal

## Getting started

`$ npm install react-native-stripe-terminal --save`

### Mostly automatic installation

`$ react-native link react-native-stripe-terminal`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-stripe-terminal` and add `RNStripeTerminal.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNStripeTerminal.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.reactlibrary.RNStripeTerminalPackage;` to the imports at the top of the file
  - Add `new RNStripeTerminalPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-stripe-terminal'
  	project(':react-native-stripe-terminal').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-stripe-terminal/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-stripe-terminal')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNStripeTerminal.sln` in `node_modules/react-native-stripe-terminal/windows/RNStripeTerminal.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Stripe.Terminal.RNStripeTerminal;` to the usings at the top of the file
  - Add `new RNStripeTerminalPackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNStripeTerminal from 'react-native-stripe-terminal';

// TODO: What to do with the module?
RNStripeTerminal;
```
  