package com.smartwear.publicwatch.ui.device.bean.diydial;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Android on 2023/3/4.
 * DIY表盘文件json解析对象
 */
public class ZhDiyDialBean implements Serializable {

    /**
     * id : c56b845adabc43a39a827700df8b7fc3
     * watchVersion : 1
     * shape : Rectangle
     * dpi : 410x502
     * thumbmaiDpi : 274x336
     * thumbmaiOffset: 68x83
     * type : DIY
     * author : JW
     * code : 007
     * watchName : Inspiration
     * watchDesc : It is an inspired guess
     * background : {"md5Background":"C7ECE09A7CEA2DD51F17E1B785AE8BBC","backgroundImgPath":"/background/background.png","md5Thumbmail":"6F015F0A734A2DA47AC423A74421758C","thumbmailImgPath":"/background/thumbmail.png","overlayImgPath":"/background/overlay.png","designsketchImgPath":"/background/designsketch.png"}
     * complex : {"isCompress":false,"md5":"F57277A2C702811BCF6B563AF88710F3","path":"/complex/complex.bin","infos":[{"location":"MidUp","detail":[{"isDefault":true,"typeName":"Kwh","pointX":152,"pointY":92,"picPath":"/complex/Kwh.png"},{"isDefault":false,"typeName":"GeneralDate","pointX":152,"pointY":92,"picPath":"/complex/GeneralDate.png"},{"isDefault":false,"typeName":"Step","pointX":152,"pointY":92,"picPath":"/complex/Step.png"},{"isDefault":false,"typeName":"Calorie","pointX":152,"pointY":92,"picPath":"/complex/Calorie.png"}]},{"location":"LeftMid","detail":[{"isDefault":true,"typeName":"Kwh","pointX":72,"pointY":198,"picPath":"/complex/Kwh.png"},{"isDefault":false,"typeName":"GeneralDate","pointX":72,"pointY":198,"picPath":"/complex/GeneralDate.png"},{"isDefault":false,"typeName":"Step","pointX":72,"pointY":198,"picPath":"/complex/Step.png"}]},{"location":"RightMid","detail":[{"isDefault":true,"typeName":"Kwh","pointX":232,"pointY":198,"picPath":"/complex/Kwh.png"},{"isDefault":false,"typeName":"GeneralDate","pointX":232,"pointY":198,"picPath":"/complex/GeneralDate.png"}]},{"location":"MidBottom","detail":[{"isDefault":true,"typeName":"Kwh","pointX":152,"pointY":304,"picPath":"/complex/Kwh.png"}]}]}
     * pointers : [{"isCompress":false,"md5":"E1B6269E0D05F7D42F53FC63E98D3AD7","pointerImgPath":"/pointer/3101_IMG.png","pointerDataPath":"/pointer/3101_Data.bin","pointerOverlayPath":"/pointer/3101_Overlay.png"},{"isCompress":false,"md5":"C942C20CAF09DB8F70692712898E7E4D","pointerImgPath":"/pointer/3102_IMG.png","pointerDataPath":"/pointer/3102_Data.bin","pointerOverlayPath":"/pointer/3102_Overlay.png"},{"isCompress":false,"md5":"8938E6F73C7045AE881F6C45772640A1","pointerImgPath":"/pointer/3103_IMG.png","pointerDataPath":"/pointer/3103_Data.bin","pointerOverlayPath":"/pointer/3103_Overlay.png"}]
     */

    private String id;
    private int watchVersion;
    private String shape;
    private String dpi;
    private String thumbnailDpi;
    private String thumbnailOffset;
    private String type;
    private String author;
    private String code;
    private String watchName;
    private String watchDesc;
    private BackgroundBean background;
    private ComplexBean complex;
    private List<PointersBean> pointers;

    private List<TimesBean> time;

    public static class BackgroundBean implements Serializable {
        /**
         * md5Background : C7ECE09A7CEA2DD51F17E1B785AE8BBC
         * backgroundImgPath : /background/background.png
         * md5Thumbmail : 6F015F0A734A2DA47AC423A74421758C
         * thumbmailImgPath : /background/thumbmail.png
         * overlayImgPath : /background/overlay.png
         * designsketchImgPath : /background/designsketch.png
         */

        private String md5Background;
        private String backgroundImgPath;
        private String md5Thumbnail;
        private String thumbnailImgPath;
        private String overlayImgPath;
        private String designsketchImgPath;

        public String getMd5Background() {
            return md5Background;
        }

        public void setMd5Background(String md5Background) {
            this.md5Background = md5Background;
        }

        public String getBackgroundImgPath() {
            return backgroundImgPath;
        }

        public void setBackgroundImgPath(String backgroundImgPath) {
            this.backgroundImgPath = backgroundImgPath;
        }

        public String getMd5Thumbnail() {
            return md5Thumbnail;
        }

        public void setMd5Thumbnail(String md5Thumbnail) {
            this.md5Thumbnail = md5Thumbnail;
        }

        public String getThumbnailImgPath() {
            return thumbnailImgPath;
        }

        public void setThumbnailImgPath(String thumbnailImgPath) {
            this.thumbnailImgPath = thumbnailImgPath;
        }

        public String getOverlayImgPath() {
            return overlayImgPath;
        }

        public void setOverlayImgPath(String overlayImgPath) {
            this.overlayImgPath = overlayImgPath;
        }

        public String getDesignsketchImgPath() {
            return designsketchImgPath;
        }

        public void setDesignsketchImgPath(String designsketchImgPath) {
            this.designsketchImgPath = designsketchImgPath;
        }

        @Override
        public String toString() {
            return "BackgroundBean{" +
                    "md5Background='" + md5Background + '\'' +
                    ", backgroundImgPath='" + backgroundImgPath + '\'' +
                    ", md5Thumbmail='" + md5Thumbnail + '\'' +
                    ", thumbnailImgPath='" + thumbnailImgPath + '\'' +
                    ", overlayImgPath='" + overlayImgPath + '\'' +
                    ", designsketchImgPath='" + designsketchImgPath + '\'' +
                    '}';
        }
    }

    public static class ComplexBean implements Serializable {
        /**
         * isCompress : false
         * md5 : F57277A2C702811BCF6B563AF88710F3
         * path : /complex/complex.bin
         * infos : [{"location":"MidUp","detail":[{"isDefault":true,"typeName":"Kwh","pointX":152,"pointY":92,"picPath":"/complex/Kwh.png"},{"isDefault":false,"typeName":"GeneralDate","pointX":152,"pointY":92,"picPath":"/complex/GeneralDate.png"},{"isDefault":false,"typeName":"Step","pointX":152,"pointY":92,"picPath":"/complex/Step.png"},{"isDefault":false,"typeName":"Calorie","pointX":152,"pointY":92,"picPath":"/complex/Calorie.png"}]},{"location":"LeftMid","detail":[{"isDefault":true,"typeName":"Kwh","pointX":72,"pointY":198,"picPath":"/complex/Kwh.png"},{"isDefault":false,"typeName":"GeneralDate","pointX":72,"pointY":198,"picPath":"/complex/GeneralDate.png"},{"isDefault":false,"typeName":"Step","pointX":72,"pointY":198,"picPath":"/complex/Step.png"}]},{"location":"RightMid","detail":[{"isDefault":true,"typeName":"Kwh","pointX":232,"pointY":198,"picPath":"/complex/Kwh.png"},{"isDefault":false,"typeName":"GeneralDate","pointX":232,"pointY":198,"picPath":"/complex/GeneralDate.png"}]},{"location":"MidBottom","detail":[{"isDefault":true,"typeName":"Kwh","pointX":152,"pointY":304,"picPath":"/complex/Kwh.png"}]}]
         */

        private boolean isCompress;
        private String md5;
        private String path;
        private List<InfosBean> infos;

        public static class InfosBean implements Serializable {
            /**
             * location : MidUp
             * detail : [{"isDefault":true,"typeName":"Kwh","pointX":152,"pointY":92,"picPath":"/complex/Kwh.png"},{"isDefault":false,"typeName":"GeneralDate","pointX":152,"pointY":92,"picPath":"/complex/GeneralDate.png"},{"isDefault":false,"typeName":"Step","pointX":152,"pointY":92,"picPath":"/complex/Step.png"},{"isDefault":false,"typeName":"Calorie","pointX":152,"pointY":92,"picPath":"/complex/Calorie.png"}]
             */

            private String location;
            private List<DetailBean> detail;

            public static class DetailBean implements Serializable {
                /**
                 * isDefault : true
                 * typeName : Kwh
                 * pointX : 152
                 * pointY : 92
                 * picPath : /complex/Kwh.png
                 */

                private boolean isDefault;
                private String typeName;
                private int pointX;
                private int pointY;
                private String picPath;

                public boolean isDefault() {
                    return isDefault;
                }

                public void setDefault(boolean aDefault) {
                    isDefault = aDefault;
                }

                public String getTypeName() {
                    return typeName;
                }

                public void setTypeName(String typeName) {
                    this.typeName = typeName;
                }

                public int getPointX() {
                    return pointX;
                }

                public void setPointX(int pointX) {
                    this.pointX = pointX;
                }

                public int getPointY() {
                    return pointY;
                }

                public void setPointY(int pointY) {
                    this.pointY = pointY;
                }

                public String getPicPath() {
                    return picPath;
                }

                public void setPicPath(String picPath) {
                    this.picPath = picPath;
                }

                @Override
                public String toString() {
                    return "DetailBean{" +
                            "isDefault=" + isDefault +
                            ", typeName='" + typeName + '\'' +
                            ", pointX=" + pointX +
                            ", pointY=" + pointY +
                            ", picPath='" + picPath + '\'' +
                            '}';
                }
            }

            public String getLocation() {
                return location;
            }

            public void setLocation(String location) {
                this.location = location;
            }

            public List<DetailBean> getDetail() {
                return detail;
            }

            public void setDetail(List<DetailBean> detail) {
                this.detail = detail;
            }

            @Override
            public String toString() {
                return "InfosBean{" +
                        "location='" + location + '\'' +
                        ", detail=" + detail +
                        '}';
            }
        }

        public boolean isCompress() {
            return isCompress;
        }

        public void setCompress(boolean compress) {
            isCompress = compress;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<InfosBean> getInfos() {
            return infos;
        }

        public void setInfos(List<InfosBean> infos) {
            this.infos = infos;
        }

        @Override
        public String toString() {
            return "ComplexBean{" +
                    "isCompress=" + isCompress +
                    ", md5='" + md5 + '\'' +
                    ", path='" + path + '\'' +
                    ", infos=" + infos +
                    '}';
        }
    }

    public static class PointersBean implements Serializable {
        /**
         * isCompress : false
         * md5 : E1B6269E0D05F7D42F53FC63E98D3AD7
         * pointerImgPath : /pointer/3101_IMG.png
         * pointerDataPath : /pointer/3101_Data.bin
         * pointerOverlayPath : /pointer/3101_Overlay.png
         */

        private boolean isCompress;
        private String md5;
        private String pointerImgPath;
        private String pointerDataPath;
        private String pointerOverlayPath;

        public boolean isCompress() {
            return isCompress;
        }

        public void setCompress(boolean compress) {
            isCompress = compress;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getPointerImgPath() {
            return pointerImgPath;
        }

        public void setPointerImgPath(String pointerImgPath) {
            this.pointerImgPath = pointerImgPath;
        }

        public String getPointerDataPath() {
            return pointerDataPath;
        }

        public void setPointerDataPath(String pointerDataPath) {
            this.pointerDataPath = pointerDataPath;
        }

        public String getPointerOverlayPath() {
            return pointerOverlayPath;
        }

        public void setPointerOverlayPath(String pointerOverlayPath) {
            this.pointerOverlayPath = pointerOverlayPath;
        }

        @Override
        public String toString() {
            return "PointersBean{" +
                    "isCompress=" + isCompress +
                    ", md5='" + md5 + '\'' +
                    ", pointerImgPath='" + pointerImgPath + '\'' +
                    ", pointerDataPath='" + pointerDataPath + '\'' +
                    ", pointerOverlayPath='" + pointerOverlayPath + '\'' +
                    '}';
        }
    }

    public static class TimesBean implements Serializable{
        /**
         * isCompress : false
         * md5 : E1B6269E0D05F7D42F53FC63E98D3AD7
         * timeImgPath : /time/3101_IMG.png
         * timeDataPath : /time/3101_Data.bin
         * timeOverlayPath : /time/3101_Overlay.png
         */
        private boolean isCompress;
        private String md5;
        private String timeImgPath;
        private String timeDataPath;
        private String timeOverlayPath;

        public boolean isCompress() {
            return isCompress;
        }

        public void setCompress(boolean compress) {
            isCompress = compress;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getTimeImgPath() {
            return timeImgPath;
        }

        public void setTimeImgPath(String timeImgPath) {
            this.timeImgPath = timeImgPath;
        }

        public String getTimeDataPath() {
            return timeDataPath;
        }

        public void setTimeDataPath(String timeDataPath) {
            this.timeDataPath = timeDataPath;
        }

        public String getTimeOverlayPath() {
            return timeOverlayPath;
        }

        public void setTimeOverlayPath(String timeOverlayPath) {
            this.timeOverlayPath = timeOverlayPath;
        }

        @Override
        public String toString() {
            return "TimeBean{" +
                    "isCompress=" + isCompress +
                    ", md5='" + md5 + '\'' +
                    ", timeImgPath='" + timeImgPath + '\'' +
                    ", timeDataPath='" + timeDataPath + '\'' +
                    ", timeOverlayPath='" + timeOverlayPath + '\'' +
                    '}';
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWatchVersion() {
        return watchVersion;
    }

    public void setWatchVersion(int watchVersion) {
        this.watchVersion = watchVersion;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getDpi() {
        return dpi;
    }

    public void setDpi(String dpi) {
        this.dpi = dpi;
    }

    public String getThumbnailDpi() {
        return thumbnailDpi;
    }

    public void setThumbnailDpi(String thumbnailDpi) {
        this.thumbnailDpi = thumbnailDpi;
    }

    public String getThumbnailOffset() {
        return thumbnailOffset;
    }

    public void setThumbnailOffset(String thumbnailOffset) {
        this.thumbnailOffset = thumbnailOffset;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getWatchName() {
        return watchName;
    }

    public void setWatchName(String watchName) {
        this.watchName = watchName;
    }

    public String getWatchDesc() {
        return watchDesc;
    }

    public void setWatchDesc(String watchDesc) {
        this.watchDesc = watchDesc;
    }

    public BackgroundBean getBackground() {
        return background;
    }

    public void setBackground(BackgroundBean background) {
        this.background = background;
    }

    public ComplexBean getComplex() {
        return complex;
    }

    public void setComplex(ComplexBean complex) {
        this.complex = complex;
    }

    public List<PointersBean> getPointers() {
        return pointers;
    }

    public void setPointers(List<PointersBean> pointers) {
        this.pointers = pointers;
    }

    public List<TimesBean> getTime() {
        return time;
    }

    public void setTime(List<TimesBean> time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "ZhDiyDialBean{" +
                "id='" + id + '\'' +
                ", watchVersion=" + watchVersion +
                ", shape='" + shape + '\'' +
                ", dpi='" + dpi + '\'' +
                ", thumbmaiDpi='" + thumbnailDpi + '\'' +
                ", thumbmaiOffset='" + thumbnailOffset + '\'' +
                ", type='" + type + '\'' +
                ", author='" + author + '\'' +
                ", code='" + code + '\'' +
                ", watchName='" + watchName + '\'' +
                ", watchDesc='" + watchDesc + '\'' +
                ", background=" + background +
                ", complex=" + complex +
                ", pointers=" + pointers +
                ", time=" + time +
                '}';
    }

    public int[] getDiyDialWH() {
        int w = 0, h = 0;
        int[] wh = new int[2];
        if (getDpi() == null || !getDpi().contains("x")) return wh;
        String[] whPx = getDpi().split("x");
        w = Integer.parseInt(whPx[0]);
        h = Integer.parseInt(whPx[1]);
        return new int[]{w, h};
    }

    public int[] getDiyDialPreviewWH() {
        int w = 0, h = 0;
        int[] wh = new int[2];
        if (getThumbnailDpi() == null || !getThumbnailDpi().contains("x")) return wh;
        String[] whPx = getThumbnailDpi().split("x");
        w = Integer.parseInt(whPx[0]);
        h = Integer.parseInt(whPx[1]);
        return new int[]{w, h};
    }

    public int[] getDiyDialPreviewOffsetWH() {
        int w = 0, h = 0;
        int[] wh = new int[2];
        if (getThumbnailOffset() == null || !getThumbnailOffset().contains("x")) return wh;
        String[] whPx = getThumbnailOffset().split("x");
        w = Integer.parseInt(whPx[0]);
        h = Integer.parseInt(whPx[1]);
        return new int[]{w, h};
    }
}
