package com.italankin.dictionary2.base

import androidx.annotation.CallSuper
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.MvpView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class AppPresenter<T : MvpView> : MvpPresenter<T>() {

    private val compositeDisposable = CompositeDisposable()

    @CallSuper
    override fun onDestroy() {
        compositeDisposable.dispose()
    }

    protected fun Disposable.disposeOnDestroy(): Disposable {
        return apply { compositeDisposable.add(this) }
    }
}
