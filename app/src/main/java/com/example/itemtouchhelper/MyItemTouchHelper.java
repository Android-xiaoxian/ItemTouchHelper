package com.example.itemtouchhelper;

import android.app.Service;
import android.graphics.Color;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * @author FANG SHIXIAN
 * @date 2020/6/23.
 * description：
 */
public class MyItemTouchHelper {

    /**
     * 已选图片拖动改变顺序，
     * 禁止添加图片的item拖拽，
     * 拖拽到添加图片的位置不响应位置交换
     */
    public static <T> void initItemTouchHelper(final RecyclerView recycler, final RecyclerView.Adapter adapter, final List<T> list) {

        ItemTouchHelper helper;
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                //某个类型的viewHolder不可拖动
//                if (viewHolder instanceof XXViewHolder){ // XXViewHolder 是不可拖动的viewHolder类型，使用的时候替换
//                    return makeMovementFlags(0,0);
//                }
                int dragFlag = 0;
                if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                    //GridLayout布局允许 上下左右 拖动
                    dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                } else if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                    //LinearLayout布局 只允许 上下 拖动
                    dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                }
                //dier个参数swipeFlags应该是拖动删除
                return makeMovementFlags(dragFlag, 0);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //滑动事件  下面注释的代码，滑动后数据和条目错乱，被舍弃
//            Collections.swap(datas,viewHolder.getAdapterPosition(),target.getAdapterPosition());
//            ap.notifyItemMoved(viewHolder.getAdapterPosition(),target.getAdapterPosition());

                //得到当拖拽的viewHolder的Position
                int fromPosition = viewHolder.getAdapterPosition();
                //拿到当前拖拽到的item的viewHolder
                int toPosition = target.getAdapterPosition();
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(list, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(list, i, i - 1);
                    }
                }
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //侧滑删除可以使用；
            }


            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            /**
             * 长按选中Item的时候开始调用
             * 长按高亮
             * @param viewHolder
             * @param actionState
             */
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {

                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    viewHolder.itemView.setBackgroundColor(Color.RED);//（拖动开始 修改item的背景色，我这里设置了红色）
                    //获取系统震动服务//震动70毫秒 (需要添加震动权限)
                    Vibrator vib = (Vibrator) recycler.getContext().getSystemService(Service.VIBRATOR_SERVICE);
                    vib.vibrate(70);
                }
                super.onSelectedChanged(viewHolder, actionState);
            }

            /**
             * 手指松开的时候还原高亮
             * @param recyclerView
             * @param viewHolder
             */
            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setBackgroundColor(Color.BLUE);//（拖动结束后恢复item的背景色，我这里故意设置了蓝色）
//                adapter.notifyDataSetChanged();  //完成拖动后刷新适配器，这样拖动后删除就不会错乱（拖动排序不是必须的）
            }
        };

        helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recycler);//ItemTouchHelper绑定recyclerView

//        ItemTouchHelperGestureListener原生不是public修饰的，需要把源码复制到项目里才能实例化
        ItemTouchHelper.ItemTouchHelperGestureListener listener = helper.getmItemTouchHelperGestureListener();

        //Handler最后是全局使用同一个，避免内存泄漏，
        // 我这里demo为了方便直接new Handler()
        setLongClickDelay(new Handler(), recycler, 100, listener);
    }


    /**
     * 修改item长按拖动的响应时间
     * @param handler Handler
     * @param v RecyclerView
     * @param delay  长按响应时间
     * @param itemTouchHelperGestureListener 修改源码后的ItemTouChHelper的itemTouchHelperGestureListener
     */
    private static void setLongClickDelay(final Handler handler, final RecyclerView v, long delay,
                                          final ItemTouchHelper.ItemTouchHelperGestureListener itemTouchHelperGestureListener) {
        final long delayMillis = delay < 50 ? 50 : delay;


        v.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            private static final int TOUCH_MAX = 50;
            MotionEvent mEvent;
            private int mLastMotionX;
            private int mLastMotionY;
            private boolean mHasPerformedLongPress = false;

            private Runnable performLongClick = new Runnable() {
                @Override
                public void run() {
//                    mHasPerformedLongPress = v.performLongClick();
                    if (mEvent != null) {
                        mHasPerformedLongPress = true;
                        //响应长按onLongPress
                        itemTouchHelperGestureListener.onLongPress(mEvent);
                        mEvent = null;
                    }
                }
            };


            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        mEvent = null;
                        handler.removeCallbacks(performLongClick);
                        // case 1
                        v.setTag(mHasPerformedLongPress);
                        v.performClick();
                        // case 2
                        /*if (!mHasPerformedLongPress) {
                            v.performClick();
                        }*/
                        break;
                    case MotionEvent.ACTION_MOVE:

                        if (Math.abs(mLastMotionX - x) > TOUCH_MAX || Math.abs(mLastMotionY - y) > TOUCH_MAX) {
                            mEvent = null;
                            handler.removeCallbacks(performLongClick);
                        }
                        break;
                    case MotionEvent.ACTION_DOWN:

                        mHasPerformedLongPress = false;
                        handler.removeCallbacks(performLongClick);
                        mLastMotionX = x;
                        mLastMotionY = y;

                        // 不能直接用mEvent = event，这样赋值 mEvent 的 getX 和 getY 得到的值是相对屏幕（或者是父布局）的，
                        // 具体原理不清楚
                        mEvent = MotionEvent.obtain(event);
                        handler.postDelayed(performLongClick, delayMillis);

                        break;
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(performLongClick);
                        break;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent event) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

//        v.setOnTouchListener(new View.OnTouchListener() {
//            private static final int TOUCH_MAX = 50;
//
//            private int mLastMotionX;
//            private int mLastMotionY;
//            private boolean mHasPerformedLongPress = false;
//
//            private Runnable performLongClick = new Runnable() {
//                @Override
//                public void run() {
//                    mHasPerformedLongPress = v.performLongClick();
//                }
//            };
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                int x = (int) event.getX();
//                int y = (int) event.getY();
//
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_UP:
//                        handler.removeCallbacks(performLongClick);
//                        // case 1
//                        v.setTag(mHasPerformedLongPress);
//                        v.performClick();
//                        // case 2
//                        /*if (!mHasPerformedLongPress) {
//                            v.performClick();
//                        }*/
//                        break;
//                    case MotionEvent.ACTION_MOVE:
//                        if (Math.abs(mLastMotionX - x) > TOUCH_MAX || Math.abs(mLastMotionY - y) > TOUCH_MAX) {
//                            handler.removeCallbacks(performLongClick);
//                        }
//                        break;
//                    case MotionEvent.ACTION_DOWN:
//                        time = System.currentTimeMillis();
//                        Log.e("test", time + "  ACTION_DOWN ");
//
//                        mHasPerformedLongPress = false;
//                        handler.removeCallbacks(performLongClick);
//                        mLastMotionX = x;
//                        mLastMotionY = y;
//                        handler.postDelayed(performLongClick, delayMillis);
//                        break;
//                }
//                return false;
//            }
//        });
    }
}

