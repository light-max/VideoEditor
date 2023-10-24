package com.lifengqiang.videoeditor.base

interface IPresenter<M, out V : IView> {
    fun model(): M
    fun view(): V?
    fun onAttachView(view: @UnsafeVariance V)
    fun onDetachView()
}
