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
    private final MutableLiveData<List<Integer>> tauxList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Integer>> psnrList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> oldSizes    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> newSizes    = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Integer> selectedTab = new MutableLiveData<>(0);

    private final MutableLiveData<Uri> zoomUri = new MutableLiveData<>();

    public void setSelectedMethod(int method) { selectedMethod.setValue(method); }
    public LiveData<Integer> getSelectedMethod() { return selectedMethod; }
    public void setQuality(int q) { quality.setValue(q); }
    public LiveData<Integer> getQuality() { return quality; }

    public LiveData<List<Uri>> getResultUris() {
        return resultUris;
    }
    public void addResultUri(Uri uri) {
        List<Uri> current = resultUris.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(uri);
        resultUris.setValue(current);
    }

    public LiveData<List<Integer>> getTauxList() {
        return tauxList;
    }
    public void addTaux(int t) {
        List<Integer> current = tauxList.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(t);
        tauxList.setValue(current);
    }

    public LiveData<List<Integer>> getPsnrList() {
        return psnrList;
    }
    public void addPsnr(int p) {
        List<Integer> current = psnrList.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(p);
        psnrList.setValue(current);
    }

    public LiveData<List<Long>> getOldSizes() {
        return oldSizes;
    }
    public void addOldSize(long s) {
        List<Long> current = oldSizes.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(s);
        oldSizes.setValue(current);
    }

    public LiveData<List<Long>> getNewSizes() {
        return newSizes;
    }
    public void addNewSize(long s) {
        List<Long> current = newSizes.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(s);
        newSizes.setValue(current);
    }

    // Quand on veut tout réinitialiser (image, résultats, etc.)
    public void clearResults() {
        resultUris.setValue(new ArrayList<>());
        tauxList.setValue(new ArrayList<>());
        psnrList.setValue(new ArrayList<>());
        oldSizes.setValue(new ArrayList<>());
        newSizes.setValue(new ArrayList<>());
        zoomUri.setValue(null);
    }
    public LiveData<Integer> getSelectedTab() {return selectedTab;}
    public void setSelectedTab(int index) { selectedTab.setValue(index); }

    public void addImgUri(Uri uri) {
        List<Uri> copy = new ArrayList<>(imgUris.getValue());
        copy.add(uri);
        imgUris.setValue(copy);
    }

    public void setImgUris(List<Uri> uris) {
        List<Uri> copy = new ArrayList<>(uris);
        imgUris.setValue(copy);
    }
    public void clearImgUris() { imgUris.setValue(new ArrayList<>()); }
    public LiveData<List<Uri>> getImgUris() { return imgUris; }

    public LiveData<Uri> getZoomUri()   { return zoomUri; }
    public void setZoomUri(Uri uri)     { zoomUri.setValue(uri); }


}
