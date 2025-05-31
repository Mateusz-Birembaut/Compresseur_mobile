package com.example.compresseur_mobile;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class CompressionViewModel extends ViewModel {
    private final MutableLiveData<List<Uri>> imgUris = new MutableLiveData<>(new ArrayList<>());



    private final MutableLiveData<Integer> selectedMethod = new MutableLiveData<>();
    private final MutableLiveData<Integer> quality = new MutableLiveData<>();

    private final MutableLiveData<List<Uri>> resultUris = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Float>> tauxList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Float>> psnrList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> oldSizes    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> newSizes    = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Integer> selectedTab = new MutableLiveData<>(0);

    private final MutableLiveData<Uri> zoomUri = new MutableLiveData<>();

    public void setSelectedMethod(int method) { selectedMethod.postValue(method); }
    public LiveData<Integer> getSelectedMethod() { return selectedMethod; }
    public void setQuality(int q) { quality.postValue(q); }
    public LiveData<Integer> getQuality() { return quality; }

    public LiveData<List<Uri>> getResultUris() {
        return resultUris;
    }
    public void addResultUri(Uri uri) {
        List<Uri> current = resultUris.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(uri);
        resultUris.postValue(current);
    }

    public LiveData<List<Float>> getTauxList() {
        return tauxList;
    }
    public void addTaux(float t) {
        List<Float> current = tauxList.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(t);
        tauxList.postValue(current);
    }

    public LiveData<List<Float>> getPsnrList() {
        return psnrList;
    }
    public void addPsnr(float p) {
        List<Float> current = psnrList.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(p);
        psnrList.postValue(current);
    }

    public LiveData<List<Long>> getOldSizes() {
        return oldSizes;
    }
    public void addOldSize(long s) {
        List<Long> current = oldSizes.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(s);
        oldSizes.postValue(current);
    }

    public LiveData<List<Long>> getNewSizes() {
        return newSizes;
    }
    public void addNewSize(long s) {
        List<Long> current = newSizes.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(s);
        newSizes.postValue(current);
    }


    public void clearResults() {
        resultUris.postValue(new ArrayList<>());
        tauxList.postValue(new ArrayList<>());
        psnrList.postValue(new ArrayList<>());
        oldSizes.postValue(new ArrayList<>());
        newSizes.postValue(new ArrayList<>());
        zoomUri.postValue(null);
    }
    public LiveData<Integer> getSelectedTab() {return selectedTab;}
    public void setSelectedTab(int index) { selectedTab.postValue(index); }

    public void addImgUri(Uri uri) {
        List<Uri> copy = new ArrayList<>(imgUris.getValue());
        copy.add(uri);
        imgUris.postValue(copy);
    }

    public void setImgUris(List<Uri> uris) {
        List<Uri> copy = new ArrayList<>(uris);
        imgUris.postValue(copy);
    }
    public void clearImgUris() { imgUris.postValue(new ArrayList<>()); }
    public LiveData<List<Uri>> getImgUris() { return imgUris; }

    public LiveData<Uri> getZoomUri()   { return zoomUri; }
    public void setZoomUri(Uri uri)     { zoomUri.postValue(uri); }



}
