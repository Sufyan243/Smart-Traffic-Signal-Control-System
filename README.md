# Smart Traffic Signal Control System

An **operating-systems scheduling simulator** disguised as a traffic intersection. The intersection
is the CPU, each vehicle is a process with an arrival time and a burst (crossing) time, and the
green light is the CPU being granted to one process at a time. The app runs **FCFS, SJF, Round
Robin and Priority + Aging** over an identical, seeded dataset, animates the chosen algorithm live
in JavaFX, and produces a side-by-side comparison dashboard plus CSV export.

> Built as a single-threaded, deterministic, tick-based discrete-event engine — so every run is
> reproducible and the animated run and the measured metrics always agree.

---

## What it demonstrates (OS concepts)

| Concept | Where it lives |
|---|---|
| CPU scheduling algorithms | `scheduling/` — `FcfsStrategy`, `SjfStrategy`, `RoundRobinStrategy`, `PriorityAgingStrategy` |
| Process model (arrival/burst/priority/state) | `domain/Vehicle` |
| Mutual exclusion / critical section | `domain/Intersection` (single occupant, `ReentrantLock`) |
| Preemption & context switching | Round Robin quantum + priority preemption in the engine |
| Starvation & aging | `PriorityAgingStrategy` (effective priority improves while waiting) |
| Priority inversion handling for emergencies | `engine/EmergencyVehicleHandler`, `NormalVehicleSuspender`, `SignalSwitcher` |
| Scheduling metrics | waiting / turnaround / response time, throughput (`metrics/`) |

## Requirements

- **JDK 17** (or newer) with `JAVA_HOME` set
- Maven is **not** required — a Maven Wrapper (`mvnw` / `mvnw.cmd`) is committed and downloads the
  correct Maven automatically on first use.

JavaFX itself is pulled in automatically (`org.openjfx`), so there is no manual SDK install.

## Run it

Windows (PowerShell / cmd):

```powershell
.\mvnw.cmd clean javafx:run
```

macOS / Linux:

```bash
./mvnw clean javafx:run
```

(If you have Maven installed globally you can use `mvn` instead of the wrapper.)

## Run the tests

```powershell
.\mvnw.cmd test
```

The tests check each scheduler's output against **hand-computed Gantt charts** (e.g. FCFS on bursts
5/3/2 → average waiting 13/3; the same dataset under SJF → 7/3), verify Round Robin rotation and
priority preemption/aging, and confirm the seeded dataset is reproducible. Current status:
**11/11 passing.**

## Build a runnable jar

```powershell
.\mvnw.cmd clean package
```

## Using the app

1. **Configure** the scenario on the left: lanes, duration, arrival rate, RR quantum, aging
   interval, seed, emergency rate, and which vehicle types appear.
2. Pick a **scheduling algorithm** and press **Start**. Use **Pause / Step / Reset** to control the
   run; the **speed** slider sets ticks per second.
3. Watch the centre canvas: vehicles queue toward the intersection, the green light follows the
   crossing vehicle, emergencies glow red, suspended vehicles grey out.
4. Press **Inject Emergency** (pick a lane) mid-run. Under **Priority + Aging** the emergency
   preempts the current vehicle and gets a green path; under the others it simply joins the queue —
   a useful contrast to show at a defense.
5. The right dashboard shows **live scheduling / traffic / performance metrics** and the final
   result of a run.
6. Press **Run Comparison** to execute all four algorithms on the *same* dataset and populate the
   **Comparison** tab (table + bar chart + **Gantt timeline** per algorithm). Press **Export CSV** to
   write `dataset.csv`, `summary.csv`, `per-vehicle.csv` and `gantt.csv` for your report.

## Architecture

```
domain/      Vehicle, Lane, Intersection (CPU/resource), SignalState, SimClock, ScenarioConfig
scheduling/  SchedulingStrategy + 4 algorithms + SchedulingAlgorithmSelector
scenario/    ScenarioManager (seeded dataset), TrafficScenarioConfigurator,
             VehicleTypeSelector, SignalParameterAdjuster
engine/      SimulationController (tick loop), SimulationRunner (headless),
             EmergencyVehicleHandler, NormalVehicleSuspender, SignalSwitcher, SimEvent
metrics/     MetricsCollector, MetricsCalculator, RunMetrics, VehicleMetrics, GanttSegment,
             AlgorithmComparator, ComparisonReportGenerator (CSV)
ui/          App entry (Launcher → App), MainView, ControlPanel, SimulationVisualizer + displayers,
             MetricsDashboard + metric/comparison displayers
```

**Data flow:** `ScenarioManager` builds the seeded dataset → for each algorithm the engine resets and
replays it identically → `SimClock` ticks → `SchedulingStrategy.selectRunning()` decides who crosses
→ `Intersection` enforces a single active vehicle → `MetricsCollector` records the Gantt timeline →
`MetricsCalculator` summarises → `ComparisonReportGenerator` exports CSV.

## Design notes / honesty for the report

- **Not "real-time scheduling".** These are classic CPU-scheduling algorithms. The closest analog to
  real-time behaviour here is Priority + Aging, not EDF/Rate-Monotonic — don't claim otherwise.
- **SJF on traffic is a teaching device.** Real queued cars can't be reordered by who crosses
  fastest; crossing time is modelled as burst time purely to demonstrate SJF behaviour.
- **Round Robin is the most realistic mapping** — real signals genuinely cycle on fixed timers.
- **FCFS is kept pure** as the control/baseline; emergency handling lives only in Priority + Aging.
- **Single-threaded on purpose.** A scheduling simulator must be deterministic; the `ReentrantLock`
  on `Intersection` makes the mutual-exclusion guarantee explicit without introducing
  race-condition non-determinism into the demo.
