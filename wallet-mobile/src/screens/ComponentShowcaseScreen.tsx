import { ReactNode, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Avatar } from '../components/Avatar';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { Input } from '../components/Input';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { OtpInput } from '../components/OtpInput';
import { StatusBadge } from '../components/StatusBadge';
import { TransactionRow } from '../components/TransactionRow';
import { useConfirm } from '../hooks/useConfirm';
import { useToast } from '../hooks/useToast';

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <View className="mb-space-xl">
      <Text className="font-headline-md text-headline-md text-on-surface mb-space-md">{title}</Text>
      {children}
    </View>
  );
}

export function ComponentShowcaseScreen() {
  const { showToast } = useToast();
  const { confirm } = useConfirm();

  const [isLoading, setIsLoading] = useState(false);
  const [otp, setOtp] = useState('');

  function handleLoadingDemo() {
    setIsLoading(true);
    setTimeout(() => setIsLoading(false), 1500);
  }

  async function handleConfirmDemo() {
    const answer = await confirm({
      title: 'Thu hồi phiên đăng nhập?',
      description: 'Thiết bị này sẽ bị đăng xuất ngay lập tức. Bạn chắc chắn muốn tiếp tục?',
      confirmLabel: 'Thu hồi',
      cancelLabel: 'Huỷ',
      destructive: true,
    });
    showToast(answer ? 'Đã thu hồi phiên đăng nhập' : 'Đã huỷ thao tác', answer ? 'success' : 'info');
  }

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <ScrollView className="flex-1 px-gutter-mobile pt-space-lg" contentContainerStyle={{ paddingBottom: 48 }}>
        <Text className="font-headline-lg text-headline-lg text-on-surface mb-space-xl">Component Showcase</Text>

        <Section title="Button">
          <View className="gap-space-sm">
            <Button label="Primary" variant="primary" onPress={() => showToast('Đã bấm Primary Button', 'success')} />
            <Button label="Secondary" variant="secondary" onPress={() => showToast('Đã bấm Secondary Button', 'info')} />
            <Button label="Text Button" variant="text" />
            <Button label={isLoading ? 'Đang xử lý...' : 'Bấm để xem loading'} loading={isLoading} onPress={handleLoadingDemo} />
            <Button label="Disabled" disabled />
          </View>
        </Section>

        <Section title="Input">
          <View className="gap-space-md">
            <Input label="Họ và tên" placeholder="Nguyễn Văn An" />
            <Input label="Mật khẩu" placeholder="••••••••" secureTextEntry />
            <Input label="Email" placeholder="you@example.com" error="Email không hợp lệ" />
          </View>
        </Section>

        <Section title="OtpInput">
          <OtpInput value={otp} onChange={setOtp} onComplete={(code) => showToast(`Mã OTP: ${code}`, 'success')} />
        </Section>

        <Section title="StatusBadge">
          <View className="flex-row gap-space-sm">
            <StatusBadge status="success" />
            <StatusBadge status="warning" />
            <StatusBadge status="danger" />
            <StatusBadge status="default" label="Nháp" />
          </View>
        </Section>

        <Section title="Card">
          <Card>
            <Text className="font-body-md text-body-md text-on-surface">Nội dung bất kỳ đặt trong Card.</Text>
          </Card>
        </Section>

        <Section title="TransactionRow">
          <View className="gap-space-xs">
            <TransactionRow icon="arrow_downward" title="Trần Thị Mai" subtitle="Nạp ví • Hôm nay, 14:32" amount={1500000} direction="in" status="success" />
            <TransactionRow icon="arrow_upward" title="Lê Hoàng Nam" subtitle="Tiền ăn trưa • Hôm qua" amount={185000} direction="out" status="success" />
            <TransactionRow icon="sync_alt" title="Chuyển tiền tới Vietcombank" subtitle="Đang xử lý • Vừa xong" amount={2000000} direction="out" status="warning" />
            <TransactionRow icon="account_balance" title="Rút tiền về thẻ" subtitle="Thất bại • Hôm nay" amount={5000000} direction="out" status="danger" />
          </View>
        </Section>

        <Section title="EmptyState">
          <Card>
            <EmptyState icon="history" title="Chưa có giao dịch nào" description="Các giao dịch của bạn sẽ hiện ở đây." />
          </Card>
        </Section>

        <Section title="LoadingSkeleton">
          <View className="gap-space-sm">
            <LoadingSkeleton height={20} width="60%" />
            <LoadingSkeleton height={44} radius={12} />
          </View>
        </Section>

        <Section title="Avatar">
          <View className="flex-row items-center gap-space-md">
            <Avatar name="Nguyễn Văn An" size={40} />
            <Avatar name="Trần Thị Mai" size={56} />
          </View>
        </Section>

        <Section title="Toast / useToast()">
          <View className="gap-space-sm">
            <Button label="Hiện toast thành công" variant="secondary" onPress={() => showToast('Thao tác thành công!', 'success')} />
            <Button label="Hiện toast lỗi" variant="secondary" onPress={() => showToast('Đã có lỗi xảy ra.', 'danger')} />
          </View>
        </Section>

        <Section title="ConfirmationDialog / useConfirm()">
          <Button label="Thu hồi phiên đăng nhập" variant="secondary" onPress={handleConfirmDemo} />
        </Section>
      </ScrollView>
    </SafeAreaView>
  );
}
