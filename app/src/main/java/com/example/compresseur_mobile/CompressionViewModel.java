package com.example.compresseur_mobile;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CompressionViewModel extends ViewModel {
    private final MutableLiveData<Uri> imgUri = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedMethod = new MutableLiveData<>();
    private final MutableLiveData<Integer> quality = new MutableLiveData<>();

    private final MutableLiveData<Uri> resultUri    = new MutableLiveData<>();
    private final MutableLiveData<Float> taux     = new MutableLiveData<>();
    private final MutableLiveData<Float> psnr     = new MutableLiveData<>();
    private final MutableLiveData<Long> oldSize     = new MutableLiveData<>();
    private final MutableLiveData<Long> newSize     = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedTab = new MutableLiveData<>(0);

    private final MutableLiveData<Uri> zoomUri = new MutableLiveData<>();

    public void setImgUri(Uri uri) { imgUri.setValue(uri); }
    public LiveData<Uri> getImgUri() { return imgUri; }
    public void setSelectedMethod(int method) { selectedMethod.setValue(method); }
    public LiveData<Integer> getSelectedMethod() { return selectedMethod; }
    public void setQuality(int q) { quality.setValue(q); }
    public LiveData<Integer> getQuality() { return quality; }
    public LiveData<Uri> getResultUri()  { return resultUri; }
    public void setResultUri(Uri u)          { resultUri.setValue(u); }
    public LiveData<Float> getTaux()      { return taux; }
    public void setTaux(float t)               { taux.setValue(t); }
    public LiveData<Float> getPsnr()      { return psnr; }
    public void setPsnr(float p)               { psnr.setValue(p); }
    public LiveData<Long>   getOldSize()    { return oldSize; }
    public void setOldSize(long s)           { oldSize.setValue(s); }
    public LiveData<Long>   getNewSize()    { return newSize; }
    public void setNewSize(long s)           { newSize.setValue(s); }
    public LiveData<Integer> getSelectedTab() {return selectedTab;}
    public void setSelectedTab(int index) { selectedTab.setValue(index); }

    public LiveData<Uri> getZoomUri()   { return zoomUri; }
    public void setZoomUri(Uri uri)     { zoomUri.setValue(uri); }

    public void clearResults() {
        resultUri.setValue(null);
        taux.setValue(null);
        psnr.setValue(null);
        oldSize.setValue(null);
        newSize.setValue(null);
        selectedTab.setValue(0);
    }


}
