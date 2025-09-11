# Prerequisites Setup

Before setting up the project, ensure that the following prerequisites are installed:

---

## 1. Install Oracle JDK 21

### Windows / Linux / macOS
1. Go to the official Oracle JDK download page:  
   👉 [Download Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)

2. Select the appropriate installer for your operating system:
   - **Windows**: `.msi` or `.zip`
   - **Linux**: `.rpm` or `.tar.gz`
   - **macOS**: `.dmg`

3. Install the JDK using the downloaded installer.

4. Verify installation by running:
   ```bash
   java -version
   ```
   Expected output should show:
   ```
   java version "21.x.x"
   ```

5. Set the `JAVA_HOME` environment variable:
   - **Windows (PowerShell)**:
     ```powershell
     setx JAVA_HOME "C:\Program Files\Java\jdk-21"
     ```
   - **Linux / macOS (bash/zsh)**:
     ```bash
     export JAVA_HOME=/usr/lib/jvm/jdk-21
     export PATH=$JAVA_HOME/bin:$PATH
     ```

---

## 2. Install Apache Maven

### Windows / Linux / macOS
1. Download Maven from the official website:  
   👉 [Download Apache Maven](https://maven.apache.org/download.cgi)

2. Extract the downloaded archive:
   - **Windows**: Unzip to `C:\Program Files\Apache\Maven`
   - **Linux / macOS**:
     ```bash
     tar -xvzf apache-maven-*.tar.gz -C /opt
     ```

3. Set the `MAVEN_HOME` environment variable and update `PATH`:
   - **Windows (PowerShell)**:
     ```powershell
     setx MAVEN_HOME "C:\Program Files\Apache\Maven\apache-maven-x.x.x"
     setx PATH "%MAVEN_HOME%\bin;%PATH%"
     ```
   - **Linux / macOS (bash/zsh)**:
     ```bash
     export MAVEN_HOME=/opt/apache-maven-x.x.x
     export PATH=$MAVEN_HOME/bin:$PATH
     ```

4. Verify installation:
   ```bash
   mvn -version
   ```
   Expected output should display Maven version along with Java 21.

---

## 3. Run the Simulation

Follow these steps to build and run the simulation:

1. Navigate to the project directory:
   ```bash
   cd edge-traffic-sim
   ```

2. Build the project and download dependencies:
   ```bash
   mvn -q -DskipTests clean package
   mvn -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\deps
   ```

3. Run the simulation with the provided configuration file:
   ```bash
   java -cp "target\edge-traffic-sim-0.1.0.jar;target\deps\*;<PATH_TO_IFOGSIM>\iFogSim\build\ifogsim2.jar;<PATH_TO_IFOGSIM>\lib\*" com.team.traffic.Runner --cfg configs\base-5x5.yaml
   ```

---

## 4. Compare Simulation Runs

The `compare_runs.py` script can be used to compare results between **TIMER** and **ACTUATED** simulations.

### Example Usage:
```bash
python compare_runs.py   --timer results/run-base-5x5-timer.csv   --actuated results/run-base-5x5-actuated.csv   --outdir results/compare-base-5x5
```

### Output:
- **summary.csv** → Tabular summary of metrics (queue length, latency, power, SLA miss, etc.)
- **Plots** → PNG files for queue length, halting %, latency, and power usage trends.

---

✅ You are now ready with **Oracle JDK 21**, **Apache Maven**, and can run the **Edge Traffic Simulation** successfully.
