### EECS 4443 Lab 3

### Group Names
- Hisham Nasir, 218882597
- Mohammed Mawi, 218621300


#### Work Breakdown

Name: Hisham Nasir
1. Implemented logic and event handling for selecting images from the gallery using intents
2. Added logic for handling permission results and error states with Toast and TextView feedback
3. Implemented lifecycle handling to restore selected images after rotation using onSaveInstanceState()
4. Designed and refined the UI layout for portrait and landscape versions
5. Consulted Android Studio Docs

Name: Mohammed Mawi
1. Initialized project and created repo
2. Designed initial layout and styling for portrait and landscape versions
3. Added logic for requesting runtime permissions for camera and storage access
4. Implemented camera functionality using MediaStore.ACTION_IMAGE_CAPTURE and `FileProvider`
5. Consulted Android Studio Docs

#### Model–View–Controller (MVC) Architecture Pattern:
- Model: Manages image data and file URIs using internal storage and lifecycle state (onSaveInstanceState and Bundle).
- View: XML layouts define the interface components
- Controller: Implemented in `MainActivity`, which handles user interactions such as taking a photo, selecting an image from the gallery, and managing permission requests and intents.
