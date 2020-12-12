package cn.archko.pdf.fragments

import androidx.fragment.app.Fragment

/**
 * @version 1.00.00
 * @description:
 * @author: archko 11-11-17
 */
abstract class RefreshableFragment : Fragment() {
    abstract fun update()
}