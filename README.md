# arcgismap
arcgis android 方面的探索 包括离线缓存 以及三维地图的探索
arcgis android差不多都快忘了，现在重新捡起来貌似还挺快的，
最开始写项目的时候都是一点一滴去试去猜，那个时候还不太会看源码，
看到不理解的地方就卡住了，现在去看那些感觉好很多了。
---------------分割线--------------------------
第一天的内容在线:加载高德地图，谷歌地图或者其他地图也是一样，主要是要去找url
第二天:缓存加载过的地图，那些切片图层其实是一张张的照片，缓存照片，每次获取的时候先从缓存里面找，没有再去网络获取。
这里研究了差不多一天的时间，之前是继承ImageTiledLayer  重写下面的方法实现的，但其实都差不多，好像这种还要简单一点，真是失策
  @Override
    protected byte[] getTile(TileKey tileKey) {
        ListenableFuture future = mLayerHelper.getFromCache(tileKey,getTileUrl(tileKey));
        try {
            return (byte[]) future.get();
        } catch (InterruptedException e) {
            if(!future.isCancelled()){
                future.cancel(true);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
继承ServiceImageTiledLayer 还要找源码手动将地图加载进去，不过之前是赶项目没仔细研究。
1.做在线地图（切片地图）最好还是继承ImageTiledLayer
2.那个url 请求返回的是一张png 图片
-------------------------分割线----------------------
3.arcgis显示手机定位！之前在arcgis上一个版本，不论是demo还是我自己写的都无法实现显示定位功能，以前居然有个傻逼的想法认为
arcgis 定位在中国用不了！但其实定位靠的是手机硬件gps或者网络定位，这其实跟位置本身在哪里没有一毛钱的关系
4.给地图加上一些东西，点，线，面
5.地图定位修正，高德等国内地图使用的是加密之后的gcj02（火星坐标系），使用atcgis自带的gps定位显示偏移量非常大，基本不在一个地方
6.LocatorTask 搜索地理位置和逆编码


  
