package ru.mathtasks.multiplicationtable

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.support.annotation.MainThread
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val mPending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<T>) {
        if (hasActiveObservers())
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")

        super.observe(owner, Observer<T> { t ->
            if (mPending.compareAndSet(true, false))
                observer.onChanged(t)
        })
    }

    override fun postValue(value: T) {
        mPending.set(true)
        super.postValue(value)
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    companion object {
        private const val TAG = "SingleLiveEvent"
    }
}