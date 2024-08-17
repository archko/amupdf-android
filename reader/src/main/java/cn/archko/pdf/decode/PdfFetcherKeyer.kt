package cn.archko.pdf.decode

import coil.key.Keyer
import coil.request.Options

/**
 * @author: archko 2024/8/17 :21:17
 */
class PdfFetcherKeyer : Keyer<PdfFetcherData> {

    override fun key(data: PdfFetcherData, options: Options): String {
        return "path_${data.path}"
    }
}
