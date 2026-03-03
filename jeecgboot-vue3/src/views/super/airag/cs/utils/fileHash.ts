import SparkMD5 from 'spark-md5';

export function computeFileMd5(file: File, chunkSize = 2 * 1024 * 1024): Promise<string> {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer();
    const reader = new FileReader();
    const chunks = Math.ceil(file.size / chunkSize);
    let current = 0;

    reader.onload = (e) => {
      spark.append(e.target!.result as ArrayBuffer);
      current++;
      if (current < chunks) {
        loadNext();
      } else {
        resolve(spark.end());
      }
    };
    reader.onerror = reject;

    function loadNext() {
      const start = current * chunkSize;
      const end = Math.min(start + chunkSize, file.size);
      reader.readAsArrayBuffer(file.slice(start, end));
    }
    loadNext();
  });
}
