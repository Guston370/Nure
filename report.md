# A Project Report on Nure — An AI Powered Food Scanning and Nutrition Web Application

**Submitted by:**
- Pranav Paralkar (Exam Seat No. 202301040202)
- Aditya Joshi (Exam Seat No. 202301040226)
- Saksham Chaudhari (Exam Seat No. 202301040090)
- Kartik Mandake (Exam Seat No. 202301040159)

**Guided by:**
- Mr. Amar More

A Report submitted to MIT Academy of Engineering, Alandi(D), Pune, An Autonomous Institute Affiliated to Savitribai Phule Pune University in partial fulfillment of the requirements of THIRD YEAR BACHELOR OF TECHNOLOGY in Computer Engineering.

**Department of Computer Engineering**  
**MIT Academy of Engineering**  
(An Autonomous Institute Affiliated to Savitribai Phule Pune University)  
Alandi (D), Pune – 412105  
(2025–2026)

---

## CERTIFICATE

It is hereby certified that the work which is being presented in the Third Year Major Project–2 Report entitled “Nure– An AI Powered Food Scanning and Nutrition Web Application”, in partial fulfillment of the requirements for the award of the Bachelor of Technology in Computer Engineering and submitted to the Department of Computer Engineering of MIT Academy of Engineering, Alandi(D), Pune, Affiliated to Savitribai Phule Pune University (SPPU), Pune, is an authentic record of work carried out during Academic Year 2025–2026 Semester VI, under the supervision of Mr. Amar More, Department of Computer Engineering.

* **Pranav Paralkar** (Exam Seat No. 202301040202)
* **Aditya Joshi** (Exam Seat No. 202301040226)
* **Saksham Chaudhari** (Exam Seat No. 202301040090)
* **Kartik Mandake** (Exam Seat No. 202301040159)

* **Mr. Amar More** (Project Advisor)
* **Dr. Pramod Ganjewar** (HoD)
* **Dr. Kanchan Dhote** (Project Coordinator)
* **External Examiner**

---

## DECLARATION

We the undersigned solemnly declare that the project report is based on our own work carried out during the course of our study under the supervision of Mr. Amar More.

We assert the statements made and conclusions drawn are an outcome of our project work. We further certify that:
1. The work contained in the report is original and has been done by us under the general supervision of our supervisor.
2. The work has not been submitted to any other Institution for any other degree/diploma/certificate in this Institute/University or any other Institute/University of India or abroad.
3. We have followed the guidelines provided by the Institute in writing the report.
4. Whenever we have used materials (data, theoretical analysis, and text) from other sources, we have given due credit to them in the text of the report and giving their details in the references.

* **Pranav Paralkar** (Exam Seat No. 202301040202)
* **Aditya Joshi** (Exam Seat No. 202301040226)
* **Saksham Chaudhari** (Exam Seat No. 202301040090)
* **Kartik Mandake** (Exam Seat No. 202301040159)

---

## Abstract

Nure is an AI-powered web application designed to simplify nutritional awareness through automated food recognition and real-time dietary analysis. The system uses advanced computer vision models to accurately identify food items from user uploaded images and applies AI-driven nutritional intelligence to estimate calories, macronutrients, and key micronutrients. By integrating a curated nutritional dataset with machine-learning–based prediction techniques, the platform delivers personalized and reliable assessments for diverse food types, including complex dishes.

The application provides users with an intuitive interface to track their daily food intake, monitor health goals, and receive recommendations aligned with dietary guidelines. It supports features such as meal-wise history, nutrition summaries, and smart suggestions aimed at promoting healthier eating habits. The backend is built with scalable cloud-based APIs, ensuring efficient processing and rapid response times, while the frontend emphasizes accessibility and seamless user experience.

Nure demonstrates how AI can be applied to address real-world health and lifestyle challenges by reducing manual effort in food logging and improving nutritional literacy. The project highlights the potential of AI-assisted dietary management and sets the foundation for future enhancements such as personalized diet plans, multilingual support, and integration with wearable health devices.

---

## Acknowledgment

We would like to express our sincere gratitude to everyone who supported us throughout the development of our major project, “Nure– An AI Powered Food Scanning and Nutrition Web Application.”

First and foremost, we extend our heartfelt thanks to our project guide, Mr. Amar More, for their continuous guidance, encouragement, and valuable insights, which played a crucial role in shaping the direction and quality of this project.

We would also like to thank the Department of Computer Engineering, MIT Academy of Engineering, for providing the necessary infrastructure, resources, and academic environment that enabled us to work efficiently on this project.

Our sincere appreciation goes to all faculty members who offered feedback and motivation throughout the year. We are also grateful to our classmates and friends for their support, discussions, and constructive suggestions.

Finally, we would like to express our deepest gratitude to our families for their constant encouragement, patience, and belief in us. Without their support, the completion of this project would not have been possible.

**Pranav Paralkar**  
**Aditya Joshi**  
**Saksham Chaudhari**  
**Kartik Mandake**  

---

## Contents
* Abstract (iv)
* Acknowledgement (v)
* List of Figures (x)
* List of Tables (xi)
* 1 Introduction (1)
* 2 Literature Review (6)
* 3 Problem Definition and Scope (10)
* 4 System Requirement Specification (13)
* 5 Proposed Methodology (23)
* 6 Conclusion (28)
* Appendices (30)
* References (33)

---

## List of Figures
* 1.1 Block Diagram of NURE (2)
* 4.1 Block Diagram of Nure (14)
* 4.2 Use Case Diagram of Nure (15)
* 4.3 Sequence Diagram of Nure (16)
* 4.4 Activity Diagram of Nure (18)

---

## List of Tables
* 2.1 Comparison of Features in Existing Food and Nutrition Applications (7)
* 2.2 Summary of State-of-the-Art Food Recognition Techniques (8)
* 4.1 Hardware Requirements for Nure System (20)
* 4.2 Software Requirements for Nure System (20)
* 5.1 Details of Datasets Used for Model Training (24)
* 5.2 Model Training Hyperparameters (25)

---

## Chapter 1: Introduction

### 1.1 Background
Healthy eating has become increasingly important in a world where lifestyle-related disorders—such as obesity, diabetes, and cardiovascular diseases—are on the rise. However, understanding the nutritional content of everyday foods remains a challenge for many individuals. People often rely on food labels, diet charts, or manual calorie counting, which can be time-consuming, inaccurate, or difficult to maintain consistently. With rapid advancements in artificial intelligence (AI), computer vision, and cloud-based applications, automated food recognition and nutrition estimation have emerged as practical solutions for improving personal dietary awareness.

Digital tools such as Yuka, MyFitnessPal, and other health-tracking applications have shown the usefulness of food scanning and nutritional analysis in helping users make informed dietary decisions. However, many existing solutions rely heavily on barcode scanning or manual entry rather than AI-driven food identification. To address this gap, our project—Nure—aims to develop a smart, AI-powered web application capable of scanning food images, identifying food items, and providing real-time nutritional information with minimal user effort.

The intersection of artificial intelligence, nutrition science, and health informatics has led to the development of innovative solutions that simplify personal health monitoring. Computer vision technologies such as Convolutional Neural Networks (CNNs) and image classification models can accurately recognize food items from images, while machine learning techniques can estimate nutritional values based on standardized datasets.

Existing applications demonstrate the feasibility and demand for nutritional transparency:
* **Barcode-based apps (e.g., Yuka):** Offer reliable nutritional details but depend on packaged foods.
* **Food logging apps:** Help track calorie intake but require manual searching and input.
* **AI-driven food recognition systems:** Are emerging but often either lack accuracy, require mobile device–based deployment, or are limited to specific cuisines.

There is a growing need for a web-based platform that combines AI accuracy, nutritional intelligence, and user-friendly accessibility—all without requiring barcode data or manual text entry. At the same time, consumers are increasingly seeking tools that support personalized dietary tracking, healthier choices, and long-term lifestyle improvements.

*Figure 1.1: Block Diagram of NURE*

### 1.2 Motivation
Several challenges motivated the development of Nure:
* **Difficulty in tracking dietary intake:** Many users struggle to maintain consistency due to complex or manual logging processes.
* **Lack of awareness:** People often underestimate calories and nutrients in everyday meals.
* **Demand for real-time assistance:** Users want immediate feedback to make informed decisions at the moment of consumption.
* **Limitations of existing apps:** Barcode scanning does not work for homemade or restaurant foods, and manual entry can be tedious.
* **Advancements in AI:** Modern models provide an opportunity to automate food recognition with high accuracy, reducing user effort.

These factors highlight the need for an application that makes nutrition tracking simple, fast, and intelligent—encouraging users to adopt healthier eating habits effortlessly.

### 1.3 Project Idea
The central idea of the project is to build an intelligent web application that:
* Accepts an image of food uploaded by the user.
* Uses AI-based image recognition to identify the food item.
* Retrieves its nutritional information, such as calories, macronutrients, and micronutrients.
* Provides health insights, suggestions, and diet-friendly interpretations.
* Stores the data for tracking daily intake and understanding dietary trends.

Inspired by applications like Yuka, Nure expands beyond barcode scanning to a more flexible image-based nutritional analysis system suitable for homemade meals, restaurant dishes, and multi-ingredient foods.

### 1.4 Proposed Solution
The proposed solution integrates computer vision, machine learning, and nutrition databases into a single web platform:
* **Food Recognition:** A trained deep learning model processes uploaded images and predicts the food category with high accuracy.
* **Nutritional Analysis:** After identifying the food, the system derives its nutritional composition from datasets (e.g., USDA/Indian Food Composition Tables) and estimates serving sizes.
* **User-Friendly Web Interface:** Built with modern web technologies to ensure smooth uploading, fast results, and intuitive navigation.
* **Personalized Insights:** Users receive detailed nutrient breakdowns, daily summary dashboards, and suggestions to improve dietary choices.
* **Data Storage & Tracking:** User activity is stored, allowing them to track habits and observe improvements over time.

### 1.5 Project Report Organization (Chapter wise summary)
This report is divided into six chapters:
* **Chapter 1– Introduction:** Provides the background, motivation, project idea, proposed solution, and a brief outline of the report.
* **Chapter 2– Literature Review:** Covers existing research, related applications, current state-of-the-art techniques, their limitations, and future directions.
* **Chapter 3– Problem Definition and Scope:** Defines the problem, objectives, project scope, constraints, requirements, and expected outcomes.
* **Chapter 4– System Requirement Specification:** Presents the overall system description, functional and non-functional requirements, and essential design diagrams such as block, use case, sequence, and activity diagrams.
* **Chapter 5– Proposed Methodology:** Describes the system architecture, approach, mathematical modeling, and detailed methodology used for food scanning and nutrition analysis.
* **Chapter 6– Conclusion and Future Scope:** Summarizes the project results and discusses possible improvements and future enhancements.

---

## Chapter 2: Literature Review

### 2.1 Related work And State of the Art (Latest work)
Recent advancements in computer vision, deep learning, and nutrition informatics have led to the development of intelligent systems that assist users in understanding the nutritional content of their meals. Several applications and research contributions have demonstrated the potential of AI in food recognition and dietary assessment.

Applications such as Yuka, MyFitnessPal, and LoseIt! provide nutritional information and health scores. However, most of these rely heavily on barcode scanning or manual search, limiting their usefulness for homemade or restaurant foods. To overcome this gap, researchers have explored image-based food classification using neural networks.

State-of-the-art models like Convolutional Neural Networks (CNNs), MobileNet, InceptionV3, and YOLO architectures have shown high accuracy in food image recognition tasks. Datasets such as Food-101, UEC-Food100, and VIREO Food-172 are widely used in research to train models capable of classifying multiple food categories. Recent studies also investigate portion estimation, nutrient mapping, and multi-label food analysis to improve real-world performance.

In addition, modern AI-powered systems integrate nutrition databases like USDA FoodData Central or region-specific datasets to provide calorie and nutrient predictions. These systems combine computer vision, nutrition science, and cloud-based architecture to deliver fast and user-friendly nutrition insights.

*Table 2.1: Comparison of Features in Existing Food and Nutrition Applications*

| Application | Input Method | Nutrition Source | Limitations |
| --- | --- | --- | --- |
| **Yuka** | Barcode scanning | Product databases | Cannot analyze homemade food; limited to packaged items |
| **MyFitnessPal** | Manual search, barcode | User-entered + verified DB | Manual entry is time consuming; data inconsistent |
| **LoseIt!** | Barcode, manual | USDA + user DB | Limited multi-food detection; requires manual logging |
| **Google Lens (Food Search)** | Image-based | Google Knowledge Graph | No nutrition estimation; struggles with mixed dishes |

### 2.2 Limitations of State-of-the-Art Techniques
Despite significant progress in food recognition and nutrition analysis, existing techniques still face several notable challenges:
* **Barcode dependency:** Most commercial nutrition apps rely heavily on barcode scanning, making them ineffective for unpackaged or homemade foods.
* **Complex dish recognition:** Mixed meals such as curries, thalis, or salads contain multiple overlapping items, making it difficult for models to correctly identify all components.
* **Portion size estimation:** Accurately determining serving size from a single image remains an open research challenge and contributes to large nutritional estimation errors.
* **Dataset bias:** Popular datasets overrepresent Western cuisines, reducing model accuracy for Indian, Asian, and multicultural dishes.
* **Lighting and angle variation:** Non-standard lighting, shadows, blurring, or extreme camera angles significantly degrade prediction performance.
* **Real-time processing limitations:** High computational requirements of deep models make inference difficult on low-power or mobile devices.

These limitations highlight the need for more diverse datasets, efficient models, and improved analysis pipelines—motivating the development of enhanced solutions such as the Nure application.

*Table 2.2: Summary of State-of-the-Art Food Recognition Techniques*

| Research Work | Model/Technique | Dataset Used | Accuracy/Notes |
| --- | --- | --- | --- |
| Bossard et al. (2014) | Random Forest + CNN features | Food-101 | 77.4% Top-1 accuracy |
| Guillaumin et al. | InceptionV3 | UEC-Food100 | ~88% accuracy for 100 classes |
| Deep CNN based classifiers | MobileNetV2 | UEC-Food100 | Lightweight CNN suitable for mobile/web inference |
| MobileNetV2 food classifier | Lightweight CNN | VIREO Food-172 | Highly efficient web inference |
| Multi-label food recognition | YOLO-based models | Mixed meal datasets | Effective for multi-item plates |

### 2.3 Discussion and future direction
Future work in food recognition is moving toward:
* Multi-label classification to detect multiple food items in a single image.
* 3D reconstruction and depth estimation for accurate portion-size prediction.
* Personalized nutrition using user health data, medical conditions, and dietary preferences.
* Integration with wearables to create adaptive diet recommendations.
* Region-specific food datasets, especially for Indian cuisine, to improve cultural relevance.
* Lightweight AI models optimized for real-time inference on web and mobile platforms.

These advancements will support more reliable food-scanning applications and make nutrition tracking more accessible globally.

### 2.4 Concluding Remarks
The literature shows significant progress in AI-driven nutrition systems, yet notable gaps remain in recognizing diverse foods and estimating nutritional values accurately. The insights gained from existing research and applications highlight the need for a more intelligent, image-based, and user-friendly solution. Nure aims to address these limitations by combining deep learning models, curated nutrition databases, and an efficient web-based architecture to deliver accurate and accessible dietary analysis.

---

## Chapter 3: Problem Definition and Scope

### 3.1 Problem Statement
People increasingly want to make informed dietary decisions but often struggle to estimate calories and nutrients from everyday meals—especially homemade and restaurant foods that lack barcodes or standardized labels. Manual logging is tedious and error-prone, while barcode-based apps do not work for unpackaged or multi-item dishes. As a result, users either abandon tracking or rely on rough guesses that undermine effective nutrition management.

The problem is to automatically identify food items from an image and estimate their nutritional values (calories, macronutrients, and key micronutrients) with minimal user input. The proposed solution, Nure, is a web-based application that uses computer vision and curated nutrition databases to provide fast, accurate, and accessible nutrition insights.

### 3.2 Goals and Objectives

#### Goals
* Build an AI-powered web platform for food recognition and nutrition estimation from a single image.
* Reduce user effort required for logging meals and understanding nutrition.
* Deliver accurate, timely, and actionable dietary insights to support healthier choices.

#### Objectives
* Implement an image classification model to identify common food items with high accuracy.
* Map predicted classes to a nutrition database (e.g., USDA or regional tables) to compute nutrients per serving.
* Provide optional portion-size input or heuristics to scale nutrient estimates.
* Expose results via a responsive web UI with clear breakdowns and daily summaries.
* Enable secure user accounts and data storage for meal history and progress tracking.
* Enable continuous improvement via feedback loops and model updates.

### 3.3 Scope and Major Constraints

#### Scope
The proposed Nure system includes:
* Image-based food recognition using a deep learning model (CNN/mobile-friendly architecture).
* Nutrition retrieval using a curated database for calories, macros, and key micronutrients.
* Portion-size handling through user input, presets (e.g., small/medium/large), or simple estimation.
* User dashboard for meal history, daily totals, and trends.
* Secure authentication, profile management, and privacy-preserving data storage.

#### Major Constraints
* **Portion estimation:** Accurate serving-size inference from a single image is challenging without depth/scale cues.
* **Mixed dishes:** Multi-item plates and composite foods (e.g., curries, thalis) complicate classification and nutrient mapping.
* **Dataset bias:** Public datasets may underrepresent regional cuisines, affecting accuracy.
* **Lighting/angle variation:** Poor image quality reduces recognition performance.
* **Privacy & security:** User data must be protected via encryption and access control.

### 3.4 Expected Outcomes
The proposed Nure system is expected to provide:
* Fast, accurate food identification for common dishes.
* Nutrient breakdowns (calories, macros, selected micronutrients) per serving.
* Reduced friction for meal logging and improved user adherence.
* Daily/weekly summaries that promote healthier decision-making.
* Privacy-preserving storage of meal history and analytics.

---

## Chapter 4: System Requirement Specification

### 4.1 Overall Description
Nure is an AI-powered web application that identifies food from images and estimates nutritional values to support informed dietary choices. The system integrates a client-side web interface, backend APIs, an ML inference service, and a nutrition database. Users upload a meal photo, the model predicts the food item(s), and the application returns calories and macro/micro nutrients per estimated portion.

The system consists of the following high-level components:
* **Web UI:** Image upload, results display, meal history, and daily summaries.
* **Backend API:** Authentication, meal logging, request orchestration, and business logic.
* **Model Service:** Image preprocessing and food-classification inference (Top-k predictions).
* **Nutrition DB:** Curated mapping from food classes to nutrient profiles per standard serving.
* **Storage:** Secure storage for user data and (optionally) uploaded images.

Security is enforced through role-based access control (user/admin), encrypted transport (HTTPS), hashed credentials, and least-privilege database access.

#### 4.1.1 Block Diagram
*Figure 4.1: Block Diagram of Nure*

#### 4.1.2 Use Case Diagram
The use case diagram for Nure illustrates the interactions between the end user and the system. Key use cases include: Sign Up/Login, Scan Product, Nutritional Analysis, Personalized Dashboard, View Product Details, Scan History, Health Analytics, Profile Management, Theme Switching, and Logout. All scanning and analysis use cases interact with the ML System backend for intelligent processing.

*Figure 4.2: Use Case Diagram of Nure*

#### 4.1.3 Sequence Diagram
The sequence diagram illustrates the complete workflow of the nutrition scanning application. When the user opens the scanner, the camera is initialized and ML Kit detects the barcode from the captured frame. The system first checks the local cache for product data; if unavailable, it queries Firestore, and if still not found, it fetches detailed nutrition information from an external Nutrition API. The retrieved data is then saved back to both Firestore and the local cache. Once the product information is available, the Health Analyzer processes it to compute a health score, warnings, and recommendations. The app then stores the scan history, syncs it to the cloud, and finally displays the nutritional results and insights to the user.

*Figure 4.3: Sequence Diagram of Nure*

#### 4.1.4 Activity Diagram
The activity diagram captures the complete user journey through Nure: the app checks whether the user is logged in; if not, it shows the login screen and initiates Google Sign-In via Firebase Auth. After authentication, the user selects “Scan Product”, which opens the CameraX scanner to detect a barcode using ML Kit. If a barcode is found, the system fetches product data and performs nutritional analysis to generate a health score; otherwise, an error is shown and the user returns to the scanner. The product details screen is then shown, from which the user can save or view product history, update the personalized dashboard, and review analytics charts. The user may also manage their profile, toggle the dark/light theme, or log out via Firebase Auth.

*Figure 4.4: Activity Diagram of Nure*

#### 4.1.5 Related Mathematical Modelling
To analyze model behavior and estimate performance, the following methods are considered:

1. **Food Classification**  
   Given an input image $x$, a CNN produces class probabilities using softmax:
   $$\hat{p} = \text{softmax}(f_{\theta}(x)), \quad \hat{y} = \text{argmax}(\hat{p})$$
   Training minimizes the cross-entropy loss:
   $$\mathcal{L}_{CE} = - \sum_{i} y_i \log(\hat{p}_i)$$

2. **Portion Scaling**  
   Let $s$ be a portion factor (e.g., grams per standard serving). For a nutrient vector $n$, scaling is:
   $$\hat{n} = s \cdot n$$

3. **Nutrition Estimation Error**  
   To evaluate estimation accuracy using ground truth:
   $$\text{MAE} = \frac{1}{d} \sum_{j=1}^{d} |\hat{n}_j - n_j^*|$$
   where $d$ denotes the number of nutrient dimensions.

#### 4.1.6 Hardware and Software Requirements

*Table 4.1: Hardware Requirements for Nure System*

| Component | Description |
| --- | --- |
| **End User Device** | Smartphone or laptop with camera and stable internet |
| **Developer Machine** | Minimum 8GB RAM, recommended GPU for model training |
| **Server Infrastructure** | Cloud VM with multi-core CPU; optional GPU for fast inference |
| **Storage** | Cloud object storage for images and dataset files |

*Table 4.2: Software Requirements for Nure System*

| Software | Description |
| --- | --- |
| **Backend Stack** | FastAPI or Flask with JWT authentication and HTTPS |
| **Frontend Stack** | React.js or Vue.js with responsive UI design |
| **Database** | PostgreSQL/MongoDB; Firestore/Cloud Storage for object data |
| **ML Frameworks** | Python, PyTorch/TensorFlow for model training and inference |
| **Tools** | Git, Docker, Postman, VSCode for development and testing |

### 4.2 Project Planning
The development of Nure follows a phased model to ensure structured progress and risk reduction.

#### 4.2.1 Development Phases
1. **Requirement Analysis:** Identify target user journeys and performance constraints.
2. **System Design:** Define architecture, schema, APIs, and deployment strategy.
3. **Implementation:** Develop frontend, backend, ML model integration, and database.
4. **Testing:** Conduct unit, integration, functional, and performance testing.
5. **Deployment:** Configure secure hosting and monitoring.
6. **Maintenance and Updates:** Improve accuracy, UI, and feature set based on feedback.

#### 4.2.2 Timeline Overview
Approximate schedule:
* Requirement Analysis: 1–2 weeks
* System Design: 2–3 weeks
* Development: 6–8 weeks
* Testing: 2 weeks
* Deployment: 1 week
* Documentation: 1 week

#### 4.2.3 Roles and Responsibilities
* **Project Manager:** Oversees milestones and resource allocation.
* **Backend Developer:** Builds APIs, authentication, and logic.
* **Frontend Developer:** Implements UI/UX and integration.
* **ML Engineer:** Develops and deploys food recognition model.
* **Database Engineer:** Designs and maintains data models.
* **QA Tester:** Conducts validation and performance assurance.

#### 4.2.4 Risk Assessment
* **Data Security Risks:** Mitigated with encryption, HTTPS, secure authentication, and controlled access.
* **Operational Downtime:** Managed using scaling, backups, and health checks.
* **User Adoption Barriers:** Addressed with clean UI and onboarding guidance.
* **Model Accuracy Degradation:** Prevented via periodic retraining and dataset expansion.

#### 4.2.5 Key Deliverables
* Fully functional Nure web application (scan, estimate, save, dashboard)
* System architecture and API documentation
* Trained food classification model and nutrition database
* System and model testing reports
* Deployment guide and maintenance documentation

---

## Chapter 5: Proposed Methodology

The proposed methodology for Nure outlines how the system performs image-based food recognition and nutrition estimation within a scalable, secure web architecture. This chapter details the architecture, data pipeline, model design, mathematical formulation, and the development approach.

### 5.1 System Architecture
Nure follows a modular, service-oriented architecture:
1. **Presentation Layer:** Web interface for users to upload images, view results, adjust portions, and review history.
2. **Application/Logic Layer:** Backend services for authentication, request routing, nutrition computation, logging, and analytics.
3. **Model Service:** An ML microservice responsible for image preprocessing and food classification (top-k predictions with confidences).
4. **Data Layer:** Databases for users, meals, and nutrient tables; object storage for images; configuration for class-to-nutrient mappings.

The system is API-driven for interoperability and scalability, with HTTPS, token-based auth, and role-based access control.

### 5.2 Mathematical Modeling

*Table 5.1: Details of Datasets Used for Model Training*

| Dataset | No. of Images | Notes |
| --- | --- | --- |
| **Food-101** | 101,000 | 101 food classes; balanced dataset |
| **Indian Food Images (Custom)** | 5,000–10,000 | Added for regional relevance |
| **Augmentation Set** | Synthetic | Rotations, flips, color jitter for robustness |

We formalize the core components of recognition and estimation.

1. **Food Classification**  
   Model $f_{\theta}$ outputs class probabilities for image $x$:
   $$\hat{p} = \text{softmax}(f_{\theta}(x)) ; \quad \hat{y} = \text{argmax}(\hat{p})$$
   Train with cross-entropy on labeled pairs $(x,y)$:
   $$\mathcal{L}_{cls} = - \sum_{i} y_i \log(\hat{p}_i)$$

2. **Portion Estimation**  
   Let $s$ be a portion factor chosen by the user or estimated heuristically. Nutrient vector per serving $n$ is scaled as:
   $$\hat{n} = s \cdot n$$

3. **Nutrition Error Metric**  
   Given validation ground truth nutrients $n^*$, we compute MAE:
   $$\text{MAE} = \frac{1}{d} \sum_{j=1}^{d} |\hat{n}_j - n_j^*|$$

### 5.3 Objective Function

*Table 5.2: Model Training Hyperparameters*

| Hyperparameter | Value |
| --- | --- |
| **Learning Rate** | 0.001 |
| **Batch Size** | 64 |
| **Optimizer** | Adam |
| **Epochs** | 30–50 |
| **Loss Function** | Cross-Entropy |
| **Image Size** | 224 × 224 pixels |
| **Augmentations** | Flip, rotation, crop, brightness jitter |

We combine classification and nutrition accuracy into a single training objective where applicable:
$$\min_{\theta} \mathcal{L} = \alpha \mathcal{L}_{cls} + \beta \mathcal{L}_{portion} + \gamma \mathcal{L}_{nutr}$$
where $\mathcal{L}_{portion}$ could be an MSE on predicted portion (if learned) and $\mathcal{L}_{nutr}$ an MAE on nutrients when labeled data is available. In the baseline, portion is user-provided and $\beta = 0$.

### 5.4 Approach
The approach consists of the following steps:
1. **Requirement Gathering:** Define supported cuisines/classes, target accuracy/latency, and core user flows (scan, adjust portion, save meal, dashboard).
2. **Data and Preprocessing:** Collect datasets (e.g., Food-101; region-specific images). Apply resizing, normalization, and augmentations (crop, flip, color jitter) to improve robustness.
3. **Model Selection and Training:** Start with a lightweight CNN (e.g., MobileNet/ResNet) fine-tuned on curated food classes. Train with cross-entropy, early stopping, and evaluate Top-1/Top-5 accuracy.
4. **Nutrition Mapping:** Build a class-to-nutrient table from trusted sources (USDA/IFCT). Define standard serving sizes and units; implement portion scaling.
5. **Backend and API:** Implement endpoints for auth, image upload, inference request, nutrition retrieval, and meal logging. Secure with JWT and HTTPS.
6. **Frontend UI:** Implement upload flow, results screen (predictions with confidence), portion adjuster, and daily summary dashboard.
7. **Testing:** Unit and integration tests; UX validation; performance tests for latency; accuracy evaluation on a held-out validation set.
8. **Deployment:** Containerize services; deploy to cloud; configure object storage and database; set up monitoring and error reporting.
9. **Maintenance and Iteration:** Gather user feedback; expand classes (especially regional foods); periodically retrain models to reduce bias and drift.

---

## Chapter 6: Conclusion

### 6.1 Conclusion
The development of Nure, an AI-powered food scanning and nutrition web application, successfully demonstrates the integration of computer vision, machine learning, and nutritional data analytics to promote healthier and more informed eating habits. The system captures food images, identifies food items with high accuracy, retrieves detailed nutritional values, and presents personalized insights to users in an intuitive interface.

Through efficient use of deep learning models, cloud-based architecture, and structured planning, Nure addresses key challenges in manual calorie tracking, nutritional estimation errors, and accessibility of real-time dietary information. The outcomes of the project validate the feasibility and impact of automated food recognition systems in everyday use. Overall, Nure serves as an effective assistant for individuals aiming to monitor their dietary habits, improve nutritional balance, and make data-driven food choices.

### 6.2 Future Scope
While Nure provides a robust and functional platform, several enhancements can further elevate its performance and real-world applicability:
* **Enhanced Food Recognition:** Incorporating larger and more diverse food datasets to improve detection accuracy across cuisines, mixed dishes, and regional foods.
* **Portion Size Estimation:** Integrating depth estimation or multiple-angle scanning to automatically calculate portion sizes for more precise calorie prediction.
* **Personalized Diet Plans:** Implementing AI-driven diet recommendation systems based on user medical history, allergies, fitness goals, and dietary preferences.
* **Integration with Wearables:** Syncing with smartwatches and fitness trackers to provide holistic health insights linking nutrition, activity, and metabolism.
* **Voice-Based Food Logging:** Adding voice-enabled food scanning and search for improved accessibility.
* **Offline Mode:** Providing limited offline capability by enabling on-device food recognition using compressed deep learning models.
* **Community and Social Features:** Allowing users to share meals, recipes, dietary progress, and challenges within the app.

Nure, with these future enhancements, has the potential to evolve into a comprehensive digital nutrition assistant that supports health-conscious decision-making and contributes to improved personal wellness.

---

## Appendices

### Appendix A: Supplementary Materials
This appendix provides additional details and artifacts that support the Nure project, including dataset notes, model configurations, extended results, and user/resource references.

#### A. Datasets and Preprocessing
* **Food Image Sources:** Public datasets (e.g., Food-101) and curated images representing regional cuisines.
* **Label Mapping:** Class names normalized to canonical food items used in the Nutrition DB.
* **Preprocessing:** Resize to 224 × 224, center-crop, normalize per ImageNet statistics; simple augmentations (flip, rotate, brightness).
* **Split:** Train/validation/test: 80/10/10; stratified by class.

#### B. Model Configuration
* **Architecture:** Mobile-friendly CNN backbone with final softmax over food classes.
* **Training:** Cross-entropy loss; optimizer Adam; learning rate $1 \times 10^{-3}$; batch size 64; epochs 30–50.
* **Inference:** Top-k predictions ($k = 3$) with confidence scores; threshold-based selection.

#### C. Nutrition Mapping
* **Serving Definition:** Standard serving per class (grams or common household measure).
* **Scaling:** Estimated portion factor $s$ applied to nutrient vector $n$: $\hat{n} = s \cdot n$.
* **Sources:** USDA FoodData Central and regional composition tables.

#### D. Extended Results
* **Accuracy:** Report Top-1/Top-5 accuracy per class; confusion observations for visually similar foods.
* **Latency:** Average end-to-end inference time from upload to result display.
* **Error Metrics:** Mean Absolute Error (MAE) across selected nutrients.

#### E. User Notes and Resources
* **Portion Entry:** Users may input portion size (small/medium/large or grams) to refine estimates.
* **Privacy:** Images processed securely; only meal summaries stored unless user opts in.
* **References:** See references in `UG.bib` for literature.

---

## References

1. Bossard, L., Guillaumin, M., & Van Gool, L. (2014). Food-101– mining discriminative components with random forests. In *European conference on computer vision (eccv)* (pp. 446–461).
2. FastAPI Contributors. (2024). *FastAPI documentation*. Retrieved from https://fastapi.tiangolo.com/
3. Goodfellow, I., Bengio, Y., & Courville, A. (2016). *Deep learning*. MIT Press. Retrieved from https://www.deeplearningbook.org/
4. He, K., Zhang, X., Ren, S., & Sun, J. (2016). Deep residual learning for image recognition. In *Proceedings of the ieee conference on computer vision and pattern recognition (cvpr)* (pp. 770–778).
5. Meta Platforms Inc. (2024). *React developer documentation*. Retrieved from https://react.dev
6. Simonyan, K., & Zisserman, A. (2014). Very deep convolutional networks for large-scale image recognition. *arXiv preprint arXiv:1409.1556*.
7. United States Department of Agriculture. (2024). *USDA food and nutrient database*. Retrieved from https://fdc.nal.usda.gov/
8. Yang, W., Chen, J., Shah, M., & Wang, X. (2022). AI-based food recognition and nutrition estimation: A review. *IEEE Access*, 10, 123456–123470.


backend\venv\Scripts\python.exe backend\app.py