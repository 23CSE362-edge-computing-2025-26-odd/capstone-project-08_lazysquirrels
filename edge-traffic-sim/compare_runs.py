# compare_runs.py
import argparse
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path

def load_agg(path: Path):
    df = pd.read_csv(path)
    needed = {'t_s','q_ns','q_ew','halting_pct','e2e_latency_ms','bytes_up','bytes_down','es1_power_w','sla_miss','policy'}
    missing = needed - set(df.columns)
    if missing:
        raise SystemExit(f"{path} missing columns: {sorted(missing)}")

    g = df.groupby('t_s', as_index=True)
    out = pd.DataFrame({
        'avg_q': g[['q_ns','q_ew']].mean().mean(axis=1),
        'avg_q_ns': g['q_ns'].mean(),
        'avg_q_ew': g['q_ew'].mean(),
        'avg_halting_pct': g['halting_pct'].mean(),
        'avg_latency_ms': g['e2e_latency_ms'].mean(),
        'sum_bytes_up': g['bytes_up'].sum(),
        'sum_bytes_down': g['bytes_down'].sum(),
        'avg_es1_power_w': g['es1_power_w'].mean(),
        'sla_miss_rate': g['sla_miss'].mean(),
    })
    out['policy'] = df['policy'].iloc[0] if 'policy' in df.columns and len(df) else 'UNK'
    return out

def summarize(name, agg):
    return {
        'policy': name,
        'avg_queue': agg['avg_q'].mean(),
        'avg_queue_ns': agg['avg_q_ns'].mean(),
        'avg_queue_ew': agg['avg_q_ew'].mean(),
        'avg_halting_pct': agg['avg_halting_pct'].mean(),
        'avg_latency_ms': agg['avg_latency_ms'].mean(),
        'total_bytes_up_MB': agg['sum_bytes_up'].sum() / (1024*1024),
        'total_bytes_down_MB': agg['sum_bytes_down'].sum() / (1024*1024),
        'avg_es1_power_w': agg['avg_es1_power_w'].mean(),
        'sla_miss_rate': agg['sla_miss_rate'].mean(),
        'ticks': len(agg),
    }

def pct_improve(baseline, new):
    return 100.0 * (baseline - new) / baseline if baseline != 0 else np.nan

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--timer", required=True, help="CSV path for TIMER run")
    ap.add_argument("--actuated", required=True, help="CSV path for ACTUATED run")
    ap.add_argument("--drle", required=True, help="CSV path for DRLE run")
    ap.add_argument("--outdir", default="results/compare", help="Output dir for summaries and plots")
    args = ap.parse_args()

    outdir = Path(args.outdir)
    outdir.mkdir(parents=True, exist_ok=True)

    timer = load_agg(Path(args.timer))
    act   = load_agg(Path(args.actuated))
    drle  = load_agg(Path(args.drle))

    # Align on common time range
    common_idx = timer.index.intersection(act.index).intersection(drle.index)
    timer = timer.loc[common_idx]
    act   = act.loc[common_idx]
    drle  = drle.loc[common_idx]

    # ---- Summary table ----
    sum_timer = summarize("TIMER", timer)
    sum_act   = summarize("ACTUATED", act)
    sum_drle  = summarize("DRLE", drle)

    # Build a comparison frame
    comp = pd.DataFrame([
        sum_timer,
        sum_act,
        sum_drle,
        {
            'policy': 'Δ% vs TIMER (positive = improvement)',
            'avg_queue': pct_improve(sum_timer['avg_queue'], sum_act['avg_queue']),
            'avg_queue_ns': pct_improve(sum_timer['avg_queue_ns'], sum_act['avg_queue_ns']),
            'avg_queue_ew': pct_improve(sum_timer['avg_queue_ew'], sum_act['avg_queue_ew']),
            'avg_halting_pct': pct_improve(sum_timer['avg_halting_pct'], sum_act['avg_halting_pct']),
            'avg_latency_ms': pct_improve(sum_timer['avg_latency_ms'], sum_act['avg_latency_ms']),
            'total_bytes_up_MB': pct_improve(sum_timer['total_bytes_up_MB'], sum_act['total_bytes_up_MB']),
            'total_bytes_down_MB': pct_improve(sum_timer['total_bytes_down_MB'], sum_act['total_bytes_down_MB']),
            'avg_es1_power_w': pct_improve(sum_timer['avg_es1_power_w'], sum_act['avg_es1_power_w']),
            'sla_miss_rate': pct_improve(sum_timer['sla_miss_rate'], sum_act['sla_miss_rate']),
            'ticks': np.nan
        },
        {
            'policy': 'Δ% vs TIMER (DRLE)',
            'avg_queue': pct_improve(sum_timer['avg_queue'], sum_drle['avg_queue']),
            'avg_queue_ns': pct_improve(sum_timer['avg_queue_ns'], sum_drle['avg_queue_ns']),
            'avg_queue_ew': pct_improve(sum_timer['avg_queue_ew'], sum_drle['avg_queue_ew']),
            'avg_halting_pct': pct_improve(sum_timer['avg_halting_pct'], sum_drle['avg_halting_pct']),
            'avg_latency_ms': pct_improve(sum_timer['avg_latency_ms'], sum_drle['avg_latency_ms']),
            'total_bytes_up_MB': pct_improve(sum_timer['total_bytes_up_MB'], sum_drle['total_bytes_up_MB']),
            'total_bytes_down_MB': pct_improve(sum_timer['total_bytes_down_MB'], sum_drle['total_bytes_down_MB']),
            'avg_es1_power_w': pct_improve(sum_timer['avg_es1_power_w'], sum_drle['avg_es1_power_w']),
            'sla_miss_rate': pct_improve(sum_timer['sla_miss_rate'], sum_drle['sla_miss_rate']),
            'ticks': np.nan
        }
    ]).set_index('policy')

    summary_csv = outdir / "summary.csv"
    comp.to_csv(summary_csv, float_format="%.4f")
    print(f"✅ Wrote summary: {summary_csv}\n")
    print(comp)

    # ---- Plots ----
    def plot_series(ycol, ylabel, fname):
        plt.figure()
        plt.plot(timer.index, timer[ycol], label="TIMER")
        plt.plot(act.index, act[ycol], label="ACTUATED")
        plt.plot(drle.index, drle[ycol], label="DRLE")
        plt.xlabel("Time (s)")
        plt.ylabel(ylabel)
        plt.legend()
        p = outdir / fname
        plt.savefig(p, bbox_inches='tight', dpi=150)
        print(f"📈 Saved {p}")

    plot_series('avg_q',          'Average Queue Length',  'avg_queue.png')
    plot_series('avg_halting_pct','Halting % (avg)',       'avg_halting_pct.png')
    plot_series('avg_latency_ms', 'End-to-end Latency (ms)','avg_latency_ms.png')
    plot_series('avg_es1_power_w','ES1 Power (W avg)',     'es1_power.png')

if __name__ == "__main__":
    main()
