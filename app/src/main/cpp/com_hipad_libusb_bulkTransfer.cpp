#include "com_hipad_libusb_bulkTransfer.h"
#include <android/log.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <unistd.h>

#define LOGI(FORMAT,...) __android_log_print(ANDROID_LOG_INFO,"wbq",FORMAT,__VA_ARGS__);
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,"wbq",FORMAT,__VA_ARGS__);

//当前C++兼容C语言
extern "C"{
#include "libusb.h"

}
FILE *fp;
char buffer[1024*100];
static void print_devs(libusb_device **devs)
{
    libusb_device *dev;
    int i = 0;

    while ((dev = devs[i++]) != NULL) {
        struct libusb_device_descriptor desc;
        int r = libusb_get_device_descriptor(dev, &desc);
        if (r < 0) {
            LOGE("%s", "failed to get device descriptor");
            return;
        }

        LOGE("%04x:%04x (bus %d, device %d)\n",
               desc.idVendor, desc.idProduct,
               libusb_get_bus_number(dev), libusb_get_device_address(dev));
    }
}
int realTransfer(int pid,int vid)
{
//    libusb_device **devs;
//    int r;
//    ssize_t cnt;
    //modify by wbq
    int ret = -1;
    ssize_t cnt;
    libusb_context *usb_context= NULL;
    libusb_device **dev_list;
    libusb_device *dev = NULL;
    libusb_device *dev_temp;
    libusb_device_handle *dev_handle = NULL;
    if (ret = libusb_init(&usb_context) < 0)
    {
        LOGE("init error:%d\n",ret);
        return ret;
    }
    if (cnt = libusb_get_device_list(usb_context, &dev_list) < 0)
    {
        LOGE("get device list error:%d\n", cnt);
        return cnt;
    }
    int m = 0;
    while ((dev_temp = dev_list[m++]) != NULL)
    {
        struct libusb_device_descriptor desc;
        int r = libusb_get_device_descriptor(dev_temp, &desc);
        if (r < 0) {
            LOGE("%s","get device descriptor error:%d");
            return NULL;
        }
        if ((desc.idProduct == pid) && (desc.idVendor == vid))
        {
            dev = dev_temp;
            break;
        }
    }
    if (dev == NULL)
    {
        LOGE("%s","find dev error\n");
        return -1;
    }


    ret = libusb_open(dev, &dev_handle);
    if (dev_handle == NULL)
    {
        LOGE("open device error:%d\n", ret);
        return ret;
    }
    ret = libusb_kernel_driver_active(dev_handle, 0);
    if (ret == 1)
    {
        libusb_detach_kernel_driver(dev_handle, 0);
        LOGE("%s","driver error\n");
    }


    if (ret = libusb_claim_interface(dev_handle, 0) < 0)
    {
        LOGE("claim interface error:%d\n",ret);
        libusb_free_device_list(dev_list, 1);
        libusb_exit(usb_context);
        return ret;
    }
    //声明描述符包括输入和输出端点
    libusb_config_descriptor *config_descriptor_in = NULL, *config_descriptor_out = NULL;
    int config = 0;
    libusb_get_configuration(dev_handle, &config);


    libusb_get_config_descriptor(dev, 0, &config_descriptor_in);
    libusb_get_config_descriptor(dev, 1, &config_descriptor_out);
//    if (config_descriptor_in)
//    {
//        unsigned char data[4096] = "aaaaaffff";
//        unsigned char data_rec[4096] = "\0";
//        int length = 0;
//        ret = libusb_bulk_transfer(dev_handle, config_descriptor_in->interface->altsetting->endpoint->bEndpointAddress, data, 9, &length, 0);
//        ret = libusb_bulk_transfer(dev_handle, 1, data, 4096, &length, 0);//host--------------->device
//        if (ret < 0) {
//            LOGE("%s","bulk transfer error\n");
//        }
//        else
//        {
//            for (int i = 0; i < 100000; i++)
//            {
//                memset(data_rec, 0, 4096);
//                ret = libusb_bulk_transfer(dev_handle, 129, data_rec, 4096, &length, 0);//device--------->host
//                LOGE("i = %d,receive data：%s",i, data_rec);
//            }
//        }
//    }
    int size = 0,i,j,res,pck_num = 1,sta=1;
    unsigned char datain[1024]="12345";
    char pkc_buff_name[100];
    while(1)
    {
        LOGE(pkc_buff_name,"/storage/emulated/0/pck/image%d.JPG",pck_num);
        fp = fopen(pkc_buff_name, "r");
        sta=1;
        for(i=0;i<40000;i++)
        {
            buffer[0]=0xF0;

            res = fread(&buffer[2], 510, 1, fp);

            if(sta == 1)
            {
                buffer[1]=0x00;
                sta=0;
            }
            else if(res == 0)
            {
                buffer[1]=0x02;
            }
            else
            {
                buffer[1]=0x01;
            }

            int rr = libusb_bulk_transfer(dev_handle,
                                          0x01,
                                          (unsigned char *) buffer,
                                          512   ,
                                          &size,
                                          1000);
            if(rr < 0)
            {
                // 	LOGE("libusb_interrupt_transfer rr%d\n",rr );
            }
            if(res == 0)
            {
                //LOGE("read end : %d\r\n",res);
                break;
            }
            if(i==0)
            {
                //        LOGE("buffer  : %d\r\n",buffer[511]);
            }
            memset(buffer,0,sizeof(buffer));
        }

        fclose(fp);
        //     LOGE("send ok\r\n" );
        pck_num++;
        if(pck_num == 117)
            pck_num = 1;

        //csd
        int rr = libusb_interrupt_transfer(dev_handle,
                                           0x83,
                                           datain,
                                           8,
                                           &size,
                                           1);
         rr = libusb_interrupt_transfer(dev_handle,
                 0x03,
                 datain,
                 10,
                 &size,
                 1000);
         if(rr < 0)
          {
              LOGE("libusb_interrupt_transfer rr%d\n",rr );
              libusb_release_interface(dev_handle, 0); //release the claimed interface
              libusb_detach_kernel_driver(dev_handle, 0);
              libusb_close(dev_handle);
              libusb_exit(NULL);
              return 0;
          }
        if(rr ==0){
            LOGE("libusb_bulk_transfer rr%d\n",rr );
            LOGE("size %d\n",size );
            LOGE("%s","data: ");
            for(j=0; j<size; j++)
                LOGE("%02x ", (unsigned char)(datain[j]));
            LOGE("%s","\n");}
        //csd
        usleep(25000);
    }
    LOGE("%s","get config_descriptor error\n");
    libusb_free_device_list(dev_list,1);
    libusb_exit(usb_context);
    //end


//
//    r = libusb_init(NULL);
//    if (r < 0)
//        return r;
//
//    cnt = libusb_get_device_list(NULL, &devs);
//    if (cnt < 0)
//        return (int) cnt;
//
//    print_devs(devs);
//    libusb_device_handle *dev_handle;         //a device handle
//    dev_handle = libusb_open_device_with_vid_pid(NULL, vid, pid); //open mouse
//    //modify wbq 1
////    int ret = libusb_open(devs[0], &dev_handle);
//
//    //end
//    if(dev_handle == NULL)
//    {
//        LOGE("%s", "Cannot open device");
//        libusb_free_device_list(devs, 1); //free the list, unref the devices in it
//        libusb_exit(NULL);                 //close the session
//        return 0;
//    }
//
//    libusb_free_device_list(devs, 1);
//    if(libusb_kernel_driver_active(dev_handle, 0) == 1)
//    { //find out if kernel driver is attached
//        LOGE("%s", "Kernel Driver Active");
//        if(libusb_detach_kernel_driver(dev_handle, 0) == 0) //detach it
//            LOGE("%s", "Kernel Driver Detached!");
//    }
//    r = libusb_claim_interface(dev_handle, 0);            //claim interface 0 (the first) of device (mine had jsut 1)
//    if(r < 0)
//    {
//        LOGE("%s", "Cannot Claim Interface1");
//        return 1;
//    }
//    LOGE("%s","openok");
//    int size = 0,i,j,res,pck_num = 1,sta=1;
//    unsigned char datain[1024]="12345";
//    char pkc_buff_name[100];
//    while(1)
//    {
//        LOGE(pkc_buff_name,"/storage/emulated/0/pck/image%d.JPG",pck_num);
//        fp = fopen(pkc_buff_name, "r");
//        sta=1;
//        for(i=0;i<40000;i++)
//        {
//            buffer[0]=0xF0;
//
//            res = fread(&buffer[2], 510, 1, fp);
//
//            if(sta == 1)
//            {
//                buffer[1]=0x00;
//                sta=0;
//            }
//            else if(res == 0)
//            {
//                buffer[1]=0x02;
//            }
//            else
//            {
//                buffer[1]=0x01;
//            }
//
//            int rr = libusb_bulk_transfer(dev_handle,
//                                          0x01,
//                                          (unsigned char *) buffer,
//                                          512   ,
//                                          &size,
//                                          1000);
//            if(rr < 0)
//            {
//                // 	LOGE("libusb_interrupt_transfer rr%d\n",rr );
//            }
//            if(res == 0)
//            {
//                //LOGE("read end : %d\r\n",res);
//                break;
//            }
//            if(i==0)
//            {
//                //        LOGE("buffer  : %d\r\n",buffer[511]);
//            }
//            memset(buffer,0,sizeof(buffer));
//        }
//
//        fclose(fp);
//        //     LOGE("send ok\r\n" );
//        pck_num++;
//        if(pck_num == 117)
//            pck_num = 1;
//
//        //csd
//        int rr = libusb_interrupt_transfer(dev_handle,
//                                           0x83,
//                                           datain,
//                                           8,
//                                           &size,
//                                           1);
///*
//         rr = libusb_interrupt_transfer(dev_handle,
//                 0x03,
//                 datain,
//                 10,
//                 &size,
//                 1000); */
//        /*  if(rr < 0)
//          {
//              LOGE("libusb_interrupt_transfer rr%d\n",rr );
//              libusb_release_interface(dev_handle, 0); //release the claimed interface
//              libusb_attach_kernel_driver(dev_handle, 0);
//              libusb_close(dev_handle);
//              libusb_exit(NULL);
//              return 0;
//          } */
//        if(rr ==0){
//            LOGE("libusb_bulk_transfer rr%d\n",rr );
//            LOGE("size %d\n",size );
//            LOGE("%s","data: ");
//            for(j=0; j<size; j++)
//                LOGE("%02x ", (unsigned char)(datain[j]));
//            LOGE("%s","\n");}
//        //csd
//        usleep(25000);
//    }
}
JNIEXPORT void JNICALL
Java_com_hipad_libusb_activity_MainActivity_libBulkTransfer(JNIEnv *env, jobject jobj,
                                                          jint pid,
                                                          jint vid){
    //java sting转成c中的string
//    const char* input_cstr=env->GetStringUTFChars(input_jstr,NULL);
//    const char* output_cstr=env->GetStringUTFChars(output_jstr,NULL);
    realTransfer(pid,vid);
//    env->ReleaseStringUTFChars(input_jstr,input_cstr);
//    env->ReleaseStringUTFChars(output_jstr,output_cstr);

}




