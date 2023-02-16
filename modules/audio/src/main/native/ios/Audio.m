/*
 * Copyright (c) 2023, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Audio.h"

JNIEnv *env;

JNIEXPORT jint JNICALL
JNI_OnLoad_Audio(JavaVM *vm, void *reserved)
{
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif
}

static int AudioInited = 0;

// Audio
Audio *_audio;

JNIEXPORT void JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_initAudio
(JNIEnv *env, jclass jClass)
{
    if (AudioInited)
    {
        return;
    }
    AudioInited = 1;
     _audio = [[Audio alloc] init];
}

JNIEXPORT jint JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_loadSoundImpl
(JNIEnv *env, jclass jClass, jstring jURL)
{
    const jchar *jURLChars = (*env)->GetStringChars(env, jURL, NULL);
    NSString *sURLChars = [NSString stringWithCharacters:(UniChar *)jURLChars length:(*env)->GetStringLength(env, jURL)];

    if (debugAttach) {
        AttachLog(@"Loading sound from file. Absolute path: %@", sURLChars);
    }

    return [_audio loadSound:sURLChars];
}

JNIEXPORT jint JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_loadMusicImpl
(JNIEnv *env, jclass jClass, jstring jURL)
{
    const jchar *jURLChars = (*env)->GetStringChars(env, jURL, NULL);
    NSString *sURLChars = [NSString stringWithCharacters:(UniChar *)jURLChars length:(*env)->GetStringLength(env, jURL)];

    if (debugAttach) {
        AttachLog(@"Loading music from file. Absolute path: %@", sURLChars);
    }

    return [_audio loadMusic:sURLChars];
}

JNIEXPORT void JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_setLooping
(JNIEnv *env, jclass jClass, jint jAudioId, jboolean jlooping)
{
// todo
}

JNIEXPORT void JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_setVolume
(JNIEnv *env, jclass jClass, jint jAudioId, jdouble jvolume)
{
// todo
}

JNIEXPORT void JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_play
(JNIEnv *env, jclass jClass, jint jAudioId)
{
    [_audio play:jAudioId];
}

JNIEXPORT void JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_pause
(JNIEnv *env, jclass jClass, jint jAudioId)
{
    [_audio pause:jAudioId];
}

JNIEXPORT void JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_stop
(JNIEnv *env, jclass jClass, jint jAudioId)
{
    [_audio stop:jAudioId];
}

JNIEXPORT void JNICALL Java_com_gluonhq_attach_audio_impl_IOSAudioService_dispose
(JNIEnv *env, jclass jClass, jint jAudioId)
{
    [_audio dispose:jAudioId];
}


@implementation Audio 

SystemSoundID soundId;
AVPlayer *avSound;
NSMutableArray *avSoundItems;

- (NSInteger) loadSound:(NSString *)fileName
{   
        [self logMessage:@"Audio sound: %@", fileName];
        AudioServicesCreateSystemSoundID((__bridge CFURLRef)[NSURL fileURLWithPath:fileName], &soundId);
        return 1;
}

- (NSInteger) loadMusic:(NSString *)fileName
{
        [self logMessage:@"Audio music: %@", fileName];
        if (!avSound)
        {
            avSound = [[AVPlayer alloc] init];
        }
        if (!avSoundItems)
        {
            avSoundItems = [[NSMutableArray alloc] init];
        }
        AVPlayerItem *item = [AVPlayerItem playerItemWithURL:[NSURL fileURLWithPath:fileName]];
        [avSoundItems addObject:item];
        [self logMessage:@"Audio item: %@, id: %d", item, [avSoundItems count] - 1];
        return [avSoundItems count] - 1;
}

- (void) play:(NSInteger)index
{
    if (soundId)
    {
        AudioServicesPlaySystemSound(soundId);
    }
    else if (avSound)
    {
        AVPlayerItem *item = [avSoundItems objectAtIndex:index];
        [self logMessage:@"Play Audio item: %@, id: %d", item, index];
        [avSound replaceCurrentItemWithPlayerItem:item];
        [item seekToTime:kCMTimeZero completionHandler:^(BOOL finished)
        {
            [avSound play];
        }];
    }
}

- (void) pause:(NSInteger)index
{
    if (avSound)
    {
        [avSound pause];
    }
}

- (void) stop:(NSInteger)index
{
    if (avSound)
    {
        [avSound seekToTime:CMTimeMake(0, 1)];
        [avSound pause];
    }
}

- (void) dispose:(NSInteger)index
{
    if (soundId > 0)
    {
        AudioServicesDisposeSystemSoundID(soundId);
        soundId = 0;
    }
    else if (avSound)
    {
        AVPlayerItem *item = [avSoundItems objectAtIndex:index];
        [item release];
    }
}

- (void) logMessage:(NSString *)format, ...;
{
    if (debugAttach)
    {
        va_list args;
        va_start(args, format);
        NSLogv([@"[Debug] " stringByAppendingString:format], args);
        va_end(args);
    }
}
@end

