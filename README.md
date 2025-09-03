# PPG Blood Pressure Monitor (PPG MoNi)

[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-2.14.0-orange.svg)](https://www.tensorflow.org/lite)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A sophisticated Android application that uses **Photoplethysmography (PPG)** signals to predict blood pressure non-invasively. The app employs advanced machine learning techniques with TensorFlow Lite models to analyze PPG data and provide accurate blood pressure predictions.

## 🎯 Key Features

### 📊 Blood Pressure Prediction
- **Dual Neural Network Architecture**: Uses both Approximate and Refinement TensorFlow Lite models
- **Non-invasive Monitoring**: Analyzes PPG signals without requiring traditional cuff-based measurements
- **Real-time Analysis**: Processes PPG data in real-time with confidence scoring
- **Multiple Health Metrics**: Calculates heart rate, SpO2, pulse wave velocity, and wave ratios

### 🔬 Advanced Signal Processing
- **PPG Data Processing**: Supports normalized and raw PPG data formats
- **Multi-channel Analysis**: Handles complex PPG signals with multiple channels
- **Signal Quality Assessment**: Automatically evaluates signal quality for reliable predictions
- **Noise Filtering**: Advanced preprocessing to improve signal quality

### 📱 Modern Android UI
- **Material Design 3**: Clean, intuitive interface following latest design guidelines
- **Interactive Charts**: Real-time visualization of PPG signals and blood pressure trends
- **Data Management**: Upload, process, and manage PPG data files
- **Multi-fragment Architecture**: Home, Charts, and Data Management screens

### 🤖 Machine Learning Integration
- **TensorFlow Lite Models**: Two-stage prediction with Approximate and Refinement networks
- **Edge Computing**: All ML inference happens on-device for privacy and speed
- **Model Optimization**: Optimized models for mobile deployment

## 🏗️ Project Architecture

### Core Components

```
app/src/main/java/com/example/ppg_moni/
├── ml/                          # Machine Learning Components
│   ├── BloodPressurePredictor.kt    # TF Lite model inference
│   └── PPGDataProcessor.kt          # ML data preprocessing
├── data/                        # Data Layer
│   ├── models/                  # Data models
│   │   ├── BloodPressurePrediction.kt
│   │   ├── PPGData.kt
│   │   └── UserProfile.kt
│   ├── database/               # Room database
│   ├── PPGDataProcessor.kt     # Core data processing
│   └── DataAnalyzer.kt         # Data analysis utilities
├── ui/                         # UI Layer
│   ├── home/                   # Home screen
│   ├── charts/                 # Charts and visualization
│   ├── data/                   # Data management
│   └── viewmodels/            # MVVM ViewModels
└── services/                   # Background services
```

### Machine Learning Pipeline

1. **Data Input**: PPG signal data (`.npy` format)
2. **Preprocessing**: Signal normalization and segmentation (1024-point segments)
3. **Feature Extraction**: Heart rate, wave ratios, pulse wave velocity
4. **Dual Model Prediction**:
   - **Approximate Network**: Initial blood pressure estimation
   - **Refinement Network**: Enhanced prediction using PPG features + approximate results
5. **Post-processing**: Confidence scoring and risk assessment

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Arctic Fox (2020.3.1) or newer
- **Android SDK**: API level 26 (Android 8.0) or higher
- **Kotlin**: Version 1.9.22
- **Gradle**: Version 8.7.3

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/BuccKyy/PPG_MoNi.git
cd PPG_MoNi
```

2. **Open in Android Studio**
```bash
# Open Android Studio and import the project
# OR use command line:
studio .
```

3. **Build the project**
```bash
./gradlew build
```

4. **Run on device/emulator**
```bash
./gradlew installDebug
```

### Project Setup

The app includes sample normalized PPG data in `app/src/main/assets/normalized_data/` for testing purposes. The TensorFlow Lite models are located in `app/src/main/assets/`:
- `ApproximateNetwork.tflite` - Initial BP prediction model
- `RefinementNetwork.tflite` - Enhanced prediction model

## 📊 Data Format

### PPG Data Input
The app supports PPG data in NumPy (`.npy`) format:
- **Raw Device Data**: Multi-channel PPG recordings
- **Normalized Data**: Preprocessed 1024-point segments (0-1 normalized)
- **Sampling Rate**: 25-100 Hz supported
- **Channels**: Single or multi-channel PPG signals

### Example Usage
```kotlin
// Load and process PPG data
val ppgProcessor = PPGDataProcessor(context)
val patientData = ppgProcessor.processNormalizedData()

// Predict blood pressure
val predictor = BloodPressurePredictor(context)
val (systolic, diastolic) = predictor.predictBloodPressure(ppgSignal)
```

## 🔬 Blood Pressure Categories

The app classifies blood pressure according to medical standards:

| Category | Systolic (mmHg) | Diastolic (mmHg) | Risk Level |
|----------|----------------|------------------|------------|
| Normal | < 120 | < 80 | Low |
| Elevated | 120-129 | < 80 | Low-Moderate |
| Stage 1 Hypertension | 130-139 | 80-89 | Moderate |
| Stage 2 Hypertension | 140-179 | 90-119 | High |
| Hypertensive Crisis | ≥ 180 | ≥ 120 | Critical |

## 🛠️ Technology Stack

### Android Development
- **Language**: Kotlin 1.9.22
- **UI Framework**: Android Views with Data Binding
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Persistence Library
- **Charts**: MPAndroidChart
- **Permissions**: Dexter for runtime permissions

### Machine Learning
- **Framework**: TensorFlow Lite 2.14.0
- **Models**: Custom trained neural networks
- **Inference**: On-device edge computing
- **Data Format**: Float32 arrays, normalized inputs

### Key Dependencies
```kotlin
// TensorFlow Lite for ML inference
implementation "org.tensorflow:tensorflow-lite:2.14.0"

// Room for local database
implementation "androidx.room:room-runtime:2.6.1"
implementation "androidx.room:room-ktx:2.6.1"

// Charts and visualization
implementation "com.github.PhilJay:MPAndroidChart:v3.1.0"

// Coroutines for async operations
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
```

## 📱 App Features

### Home Screen
- Welcome interface with quick access to main functions
- Latest blood pressure reading display
- Quick navigation to data upload and charts

### Data Management
- **File Upload**: Support for `.npy` PPG data files
- **Processing Pipeline**: Automated data preprocessing and segmentation
- **Sample Data Testing**: Built-in test data for validation
- **Progress Tracking**: Real-time processing status updates

### Charts & Visualization
- **PPG Signal Display**: Real-time waveform visualization
- **Blood Pressure Trends**: Historical BP data with trend analysis
- **SpO2 Monitoring**: Oxygen saturation calculations
- **Data Selection**: Choose specific sessions for analysis

### Analysis Results
- **Blood Pressure Values**: Systolic/Diastolic measurements
- **Confidence Scoring**: ML model confidence (0-100%)
- **Health Categories**: Automatic BP classification
- **Risk Assessment**: Personalized risk level evaluation
- **Recommendations**: Health advice based on results

## 🧪 Data Analysis Tools

The project includes Python scripts for data analysis:

### `analyze_bp_data.py`
- Analyzes PPG files and classifies blood pressure levels
- Simulates realistic BP predictions using advanced algorithms
- Provides statistical breakdown of data categories

### `normalize_device_data.py`
- Preprocesses raw device data into normalized format
- Converts multi-channel PPG data into 1024-point segments
- Handles batch processing of multiple files

## 🔬 Research Background

This project is based on photoplethysmography (PPG) technology, which:
- **Non-invasive**: Uses optical sensors to detect blood volume changes
- **Continuous Monitoring**: Enables real-time cardiovascular monitoring
- **Machine Learning Enhanced**: Uses AI to extract blood pressure from PPG signals
- **Mobile-Friendly**: Optimized for smartphone and wearable device integration

### Scientific Approach
The dual neural network architecture follows recent research in:
- PPG-based cardiovascular monitoring
- Deep learning for physiological signal processing
- Mobile health (mHealth) applications
- Non-invasive blood pressure estimation

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit changes**: `git commit -m 'Add amazing feature'`
4. **Push to branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Development Guidelines
- Follow Kotlin coding standards
- Add unit tests for new features
- Update documentation for API changes
- Ensure ML models remain optimized for mobile

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📚 References

- PPG-based blood pressure monitoring research
- TensorFlow Lite mobile ML deployment
- Android Health API guidelines
- Cardiovascular signal processing techniques

## 🙏 Acknowledgments

- TensorFlow team for mobile ML frameworks
- Android development community
- PPG signal processing research community
- Open source contributors

## 📞 Contact

For questions, suggestions, or collaborations:

- **Project Repository**: [https://github.com/BuccKyy/PPG_MoNi](https://github.com/BuccKyy/PPG_MoNi)
- **Issues**: Please use GitHub Issues for bug reports and feature requests
- **Documentation**: Additional docs available in the `/docs` directory

---

**Note**: This application is for research and educational purposes. Always consult healthcare professionals for medical decisions. The app provides estimations and should not replace professional medical devices or advice.
