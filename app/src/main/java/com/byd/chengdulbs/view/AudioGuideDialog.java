package com.byd.chengdulbs.view;

import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.byd.chengdulbs.R;
import com.byd.chengdulbs.model.BuildingModel;
import com.byd.chengdulbs.model.SubtitleItem;
import com.byd.chengdulbs.util.SrtParser;

import java.util.List;

// 1. 改为继承 Dialog (普通的弹窗)
public class AudioGuideDialog extends Dialog {

    // ❌ 删掉这些散装参数
    //    private final String title;
    //    private final String audioAsset;
    //    private final String srtAsset;
    //✅ 改用一个对象
    private final BuildingModel building;
    private final Context context;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTask;

    private RecyclerView rvSubtitles;
    private SubtitleAdapter adapter;
    private ImageView btnPlayPause;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime;

    private boolean isUserScrolling = false;

    // 构造函数接收 BuildingModel
    public AudioGuideDialog(@NonNull Context context, BuildingModel building) {
        super(context);
        this.context = context;
        this.building = building; // 保存对象
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_center_audio); // 使用新的居中布局

        // 2. 关键设置：让弹窗背景透明，宽度占屏幕 85%
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.85); // 宽度为屏幕的85%
            getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setDimAmount(0.6f); // 背景变暗程度
        }

        initView();
        initData();
    }

    private void initView() {
        TextView tvTitle = findViewById(R.id.tv_dialog_title);
        // 使用通俗名，如果没有则用正式名
        tvTitle.setText(building.getCommonName() != null ? building.getCommonName() : building.getName());

        // ▼▼▼▼▼ 设置新加的信息 ▼▼▼▼▼
        TextView tvId = findViewById(R.id.tv_detail_id);
        TextView tvInfo = findViewById(R.id.tv_detail_info);

        // 设置 ID (比如 "NO.1")
        tvId.setText("NO." + building.getId());

        // 拼接 部门 | 面积
        StringBuilder info = new StringBuilder();
        if (building.getDept() != null && !building.getDept().isEmpty()) {
            info.append(building.getDept());
        }
        if (building.getArea() != null && !building.getArea().isEmpty()) {
            if (info.length() > 0) info.append("  |  "); // 加分隔符
            info.append(building.getArea()).append(" m²");
        }
        tvInfo.setText(info.toString());

        findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());

        rvSubtitles = findViewById(R.id.rv_subtitles);
        // 使用 CenterLayoutManager 让选中的项永远居中 (代码在下面内部类)
        rvSubtitles.setLayoutManager(new CenterLayoutManager(context));

        rvSubtitles.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isUserScrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    handler.postDelayed(() -> isUserScrolling = false, 3000);
                }
            }
        });

        btnPlayPause = findViewById(R.id.btn_play_pause);
        seekBar = findViewById(R.id.seek_bar);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);

        btnPlayPause.setOnClickListener(v -> togglePlay());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { isUserScrolling = true; }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserScrolling = false;
                if (mediaPlayer != null) mediaPlayer.seekTo(seekBar.getProgress());
            }
        });
    }

    private void initData() {
        // ▼▼▼▼▼▼▼▼ 修改点：从 building 对象中获取路径 ▼▼▼▼▼▼▼▼
        String srtPath = building.getSrtPath();   // 获取字幕路径
        String audioPath = building.getAudioPath(); // 获取音频路径
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        // 加载字幕 (使用获取到的 srtPath)
        List<SubtitleItem> subtitles = SrtParser.parseSrt(context, srtPath);
        adapter = new SubtitleAdapter(subtitles);
        rvSubtitles.setAdapter(adapter);

        // 加载音频 (使用获取到的 audioPath)
        try {
            mediaPlayer = new MediaPlayer();
            AssetFileDescriptor afd = context.getAssets().openFd(audioPath);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();

            seekBar.setMax(mediaPlayer.getDuration());
            tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));

            mediaPlayer.start();
            updatePlayIcon(true);
            startUpdateTimer();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // 点击字幕跳转
        adapter.setOnItemClickListener(time -> {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo((int) time);
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    updatePlayIcon(true);
                    startUpdateTimer();
                }
            }
        });
    }

    private void togglePlay() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updatePlayIcon(false);
        } else {
            mediaPlayer.start();
            updatePlayIcon(true);
            startUpdateTimer();
        }
    }

    private void updatePlayIcon(boolean isPlaying) {
        btnPlayPause.setImageResource(isPlaying ?
                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void startUpdateTimer() {
        updateTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    tvCurrentTime.setText(formatTime(currentPosition));

                    int index = adapter.updateCurrentTime(currentPosition);
                    if (index != -1 && !isUserScrolling) {
                        smoothScrollToPosition(index);
                    }
                    handler.postDelayed(this, 100); // 提高刷新频率让进度条更顺滑
                }
            }
        };
        handler.post(updateTask);
    }

    // 让选中的 item 滚动到 RecyclerView 的垂直正中间
    private void smoothScrollToPosition(int position) {
        RecyclerView.LayoutManager layoutManager = rvSubtitles.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearSmoothScroller smoothScroller = new LinearSmoothScroller(context) {
                // ▼▼▼▼▼▼ 核心修改开始 ▼▼▼▼▼▼
                // 我们通过重写 calculateDtToFit 来手动计算“居中”的距离
                // 这样就不需要用那个报错的 SNAP_TO_CENTER 了
                @Override
                public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                    return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
                }
                // ▲▲▲▲▲▲ 核心修改结束 ▲▲▲▲▲▲
            };
            smoothScroller.setTargetPosition(position);
            layoutManager.startSmoothScroll(smoothScroller);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateTask);
    }

    // ================== Adapter (带高亮动画) ==================
    class SubtitleAdapter extends RecyclerView.Adapter<SubtitleAdapter.ViewHolder> {
        private List<SubtitleItem> list;
        private int activeIndex = -1;
        private OnItemClickListener listener;

        public SubtitleAdapter(List<SubtitleItem> list) { this.list = list; }
        public void setOnItemClickListener(OnItemClickListener listener) { this.listener = listener; }

        public int updateCurrentTime(long time) {
            int newIndex = -1;
            for (int i = 0; i < list.size(); i++) {
                if (time >= list.get(i).startTime && time <= list.get(i).endTime) {
                    newIndex = i; break;
                }
                if (i < list.size() - 1 && time > list.get(i).endTime && time < list.get(i+1).startTime) {
                    newIndex = i; break;
                }
            }
            if (newIndex != activeIndex && newIndex != -1) {
                int oldIndex = activeIndex;
                activeIndex = newIndex;
                notifyItemChanged(oldIndex);
                notifyItemChanged(activeIndex);
                return activeIndex;
            }
            return -1;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subtitle, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SubtitleItem item = list.get(position);
            holder.tvText.setText(item.text);

            boolean isActive = (position == activeIndex);

            // 获取绿条控件 (需要在 ViewHolder 里先 findViewById)
            // 如果你的 XML 里加了 view_indicator，这里就要处理
            if (holder.indicator != null) {
                holder.indicator.setVisibility(isActive ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
            }

            if (isActive) {
                holder.tvText.setTextColor(Color.WHITE);
                holder.tvText.setTextSize(20); // 当前行变大
                holder.tvText.setTypeface(null, Typeface.BOLD);
                holder.tvText.setAlpha(1.0f);
            } else {
                holder.tvText.setTextColor(Color.WHITE);
                holder.tvText.setTextSize(16); // 其他行变小
                holder.tvText.setTypeface(null, Typeface.NORMAL);
                holder.tvText.setAlpha(0.5f);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item.startTime);
            });
        }

        @Override public int getItemCount() { return list == null ? 0 : list.size(); }

        // 记得在 ViewHolder 里加回 indicator
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvText;
            android.view.View indicator; // 新增

            public ViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
                tvText = itemView.findViewById(R.id.tv_subtitle_text);
                // 如果 xml 里加了 id 为 view_indicator 的 View，这里要绑定
                indicator = itemView.findViewById(R.id.view_indicator);
            }
        }
    }

    interface OnItemClickListener { void onItemClick(long time); }

    // 辅助类：确保 smoothScrollToPosition 居中
    static class CenterLayoutManager extends LinearLayoutManager {
        public CenterLayoutManager(Context context) { super(context); }
    }
}