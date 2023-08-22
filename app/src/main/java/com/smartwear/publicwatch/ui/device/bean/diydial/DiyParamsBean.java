package com.smartwear.publicwatch.ui.device.bean.diydial;

import android.graphics.Bitmap;

import com.zhapp.ble.bean.DiyWatchFaceConfigBean;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Android on 2023/3/4.
 * Diy表盘获取数据请求参数
 */
public class DiyParamsBean implements Serializable {
    /**
     * json配置文件
     */
    private String jsonStr;

    /**
     * 背景
     */
    private BackgroundResBean backgroundResBean;

    /**
     * 指针 或者 数字 资源
     */
    private StyleResBean styleResBean;


    /**
     * 复杂功能
     */
    private FunctionsResBean functionsResBean;

    /**
     * 复杂功能设置
     */
    private DiyWatchFaceConfigBean diyWatchFaceConfigBean;

    public String getJsonStr() {
        return jsonStr;
    }

    public void setJsonStr(String jsonStr) {
        this.jsonStr = jsonStr;
    }

    public BackgroundResBean getBackgroundResBean() {
        return backgroundResBean;
    }

    public void setBackgroundResBean(BackgroundResBean backgroundResBean) {
        this.backgroundResBean = backgroundResBean;
    }

    public StyleResBean getStyleResBean() {
        return styleResBean;
    }

    public void setStyleResBean(StyleResBean styleResBean) {
        this.styleResBean = styleResBean;
    }

    public FunctionsResBean getFunctionsResBean() {
        return functionsResBean;
    }

    public void setFunctionsResBean(FunctionsResBean functionsResBean) {
        this.functionsResBean = functionsResBean;
    }

    public DiyWatchFaceConfigBean getDiyWatchFaceConfigBean() {
        return diyWatchFaceConfigBean;
    }

    public void setDiyWatchFaceConfigBean(DiyWatchFaceConfigBean diyWatchFaceConfigBean) {
        this.diyWatchFaceConfigBean = diyWatchFaceConfigBean;
    }

    @Override
    public String toString() {
        return "DiyParamsBean{" +
                "jsonStr='" + jsonStr + '\'' +
                ", backgroundResBean=" + backgroundResBean +
                ", pointerResBean=" + styleResBean +
                ", functionsResBean=" + functionsResBean +
                ", diyWatchFaceConfigBean=" + diyWatchFaceConfigBean +
                '}';
    }

    public static class BackgroundResBean {
        /**
         * 背景图资源
         */
        private Bitmap background;

        /**
         * 背景覆盖图资源
         */
        private Bitmap backgroundOverlay;

        public Bitmap getBackground() {
            return background;
        }

        public void setBackground(Bitmap background) {
            this.background = background;
        }

        public Bitmap getBackgroundOverlay() {
            return backgroundOverlay;
        }

        public void setBackgroundOverlay(Bitmap backgroundOverlay) {
            this.backgroundOverlay = backgroundOverlay;
        }

        @Override
        public String toString() {
            return "BackgroundResBean{" +
                    "background=" + background +
                    ", backgroundOverlay=" + backgroundOverlay +
                    '}';
        }
    }


    public static class StyleResBean {

        /**
         * 类型
         * @see StyleType
         */
        private int type;

        /**
         * 指针图资源
         */
        private Bitmap styleBm;

        /**
         * 背景图资源
         */
        private byte[] styleBin;

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public Bitmap getStyleBm() {
            return styleBm;
        }

        public void setStyleBm(Bitmap styleBm) {
            this.styleBm = styleBm;
        }

        public byte[] getStyleBin() {
            return styleBin;
        }

        public void setStyleBin(byte[] styleBin) {
            this.styleBin = styleBin;
        }

        @Override
        public String toString() {
            return "PointerResBean{" +
                    "pointer=" + styleBm +
                    ", pointerBin=" + Arrays.toString(styleBin) +
                    '}';
        }

        public enum StyleType {
            /**
             * 指针
             */
            POINTER(0x01),
            /**
             * 数字
             */
            NUMBER(0x02);

            private final int type;

            private StyleType(int type) {
                this.type = type;
            }

            public final int getType() {
                return type;
            }

            public static StyleType intToEnum(int state) {
                for (StyleType enumState : values()) {
                    if (enumState.getType() == state) {
                        return enumState;
                    }
                }
                return POINTER;
            }
        }
    }


    public static class FunctionsResBean {
        /**
         * 背景图资源
         */
        private byte[] functionsBin;

        private List<FunctionsBitmapBean> functionsBitmaps;

        public static class FunctionsBitmapBean {
            /**
             * 功能图资源
             */
            private Bitmap bitmap;

            /**
             * 功能名称
             * @see com.zhapp.ble.callback.DiyWatchFaceCallBack.DiyWatchFaceFunction
             */
            private int function;

            public Bitmap getBitmap() {
                return bitmap;
            }

            public void setBitmap(Bitmap bitmap) {
                this.bitmap = bitmap;
            }

            public int getFunction() {
                return function;
            }

            public void setFunction(int function) {
                this.function = function;
            }

            @Override
            public String toString() {
                return "FunctionsBitmapBean{" +
                        "bitmap=" + bitmap +
                        ", function=" + function +
                        '}';
            }
        }

        public byte[] getFunctionsBin() {
            return functionsBin;
        }

        public void setFunctionsBin(byte[] functionsBin) {
            this.functionsBin = functionsBin;
        }

        public List<FunctionsBitmapBean> getFunctionsBitmaps() {
            return functionsBitmaps;
        }

        public void setFunctionsBitmaps(List<FunctionsBitmapBean> functionsBitmaps) {
            this.functionsBitmaps = functionsBitmaps;
        }

        @Override
        public String toString() {
            return "FunctionsResBean{" +
                    "functionsBin=" + Arrays.toString(functionsBin) +
                    ", functionsBitmaps=" + functionsBitmaps +
                    '}';
        }
    }
}
